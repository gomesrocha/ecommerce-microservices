from __future__ import annotations

import argparse
import math
from pathlib import Path

import pandas as pd


REQUIRED_FILES = {
    "orders": "olist_orders_dataset.csv",
    "items": "olist_order_items_dataset.csv",
    "customers": "olist_customers_dataset.csv",
    "sellers": "olist_sellers_dataset.csv",
    "products": "olist_products_dataset.csv",
}


def mode_or_none(series: pd.Series):
    values = series.dropna()
    if values.empty:
        return None
    return values.mode().iloc[0]


def load_csv(raw_dir: Path, filename: str) -> pd.DataFrame:
    path = raw_dir / filename
    if not path.exists():
        raise FileNotFoundError(f"Arquivo não encontrado: {path}")
    return pd.read_csv(path)


def prepare_delivery_dataset(raw_dir: Path, processed_dir: Path, max_delivery_days: int) -> tuple[Path, Path]:
    processed_dir.mkdir(parents=True, exist_ok=True)

    orders = load_csv(raw_dir, REQUIRED_FILES["orders"])
    items = load_csv(raw_dir, REQUIRED_FILES["items"])
    customers = load_csv(raw_dir, REQUIRED_FILES["customers"])
    sellers = load_csv(raw_dir, REQUIRED_FILES["sellers"])
    products = load_csv(raw_dir, REQUIRED_FILES["products"])

    # =========================
    # Orders
    # =========================
    orders = orders[
        [
            "order_id",
            "customer_id",
            "order_status",
            "order_purchase_timestamp",
            "order_delivered_customer_date",
            "order_estimated_delivery_date",
        ]
    ].copy()

    orders = orders[orders["order_status"] == "delivered"].copy()

    orders["order_purchase_timestamp"] = pd.to_datetime(
        orders["order_purchase_timestamp"], errors="coerce"
    )
    orders["order_delivered_customer_date"] = pd.to_datetime(
        orders["order_delivered_customer_date"], errors="coerce"
    )
    orders["order_estimated_delivery_date"] = pd.to_datetime(
        orders["order_estimated_delivery_date"], errors="coerce"
    )

    orders = orders.dropna(
        subset=["order_purchase_timestamp", "order_delivered_customer_date"]
    )

    orders["delivery_days"] = (
        orders["order_delivered_customer_date"] - orders["order_purchase_timestamp"]
    ).dt.total_seconds() / 86400

    orders["estimated_delivery_days_olist"] = (
        orders["order_estimated_delivery_date"] - orders["order_purchase_timestamp"]
    ).dt.total_seconds() / 86400

    orders["delivery_days"] = orders["delivery_days"].apply(math.ceil)
    orders["estimated_delivery_days_olist"] = orders["estimated_delivery_days_olist"].apply(
        lambda value: math.ceil(value) if pd.notna(value) else None
    )

    orders = orders[
        (orders["delivery_days"] >= 0)
        & (orders["delivery_days"] <= max_delivery_days)
    ].copy()

    # =========================
    # Customers -> destination_state
    # =========================
    customers = customers[["customer_id", "customer_state"]].copy()
    customers = customers.rename(columns={"customer_state": "destination_state"})

    # =========================
    # Items + sellers + products
    # =========================
    products = products[
        [
            "product_id",
            "product_category_name",
            "product_weight_g",
            "product_length_cm",
            "product_height_cm",
            "product_width_cm",
        ]
    ].copy()

    products["product_volume_cm3"] = (
        products["product_length_cm"].fillna(0)
        * products["product_height_cm"].fillna(0)
        * products["product_width_cm"].fillna(0)
    )

    sellers = sellers[["seller_id", "seller_state"]].copy()
    sellers = sellers.rename(columns={"seller_state": "origin_state"})

    items = items[
        [
            "order_id",
            "order_item_id",
            "product_id",
            "seller_id",
            "price",
            "freight_value",
        ]
    ].copy()

    item_features = (
        items.merge(sellers, on="seller_id", how="left")
        .merge(products, on="product_id", how="left")
    )

    order_items_agg = (
        item_features.groupby("order_id")
        .agg(
            origin_state=("origin_state", mode_or_none),
            items_quantity=("order_item_id", "count"),
            order_value=("price", "sum"),
            freight_value=("freight_value", "sum"),
            avg_product_weight_g=("product_weight_g", "mean"),
            max_product_weight_g=("product_weight_g", "max"),
            avg_product_volume_cm3=("product_volume_cm3", "mean"),
            max_product_volume_cm3=("product_volume_cm3", "max"),
            product_category_name=("product_category_name", mode_or_none),
        )
        .reset_index()
    )

    # =========================
    # Final dataset
    # =========================
    dataset = (
        orders.merge(customers, on="customer_id", how="inner")
        .merge(order_items_agg, on="order_id", how="inner")
    )

    dataset = dataset.dropna(
        subset=[
            "origin_state",
            "destination_state",
            "items_quantity",
            "order_value",
            "freight_value",
            "delivery_days",
        ]
    )

    dataset["avg_product_weight_g"] = dataset["avg_product_weight_g"].fillna(0)
    dataset["max_product_weight_g"] = dataset["max_product_weight_g"].fillna(0)
    dataset["avg_product_volume_cm3"] = dataset["avg_product_volume_cm3"].fillna(0)
    dataset["max_product_volume_cm3"] = dataset["max_product_volume_cm3"].fillna(0)
    dataset["product_category_name"] = dataset["product_category_name"].fillna("unknown")

    dataset = dataset[
        [
            "order_id",
            "origin_state",
            "destination_state",
            "items_quantity",
            "order_value",
            "freight_value",
            "avg_product_weight_g",
            "max_product_weight_g",
            "avg_product_volume_cm3",
            "max_product_volume_cm3",
            "product_category_name",
            "estimated_delivery_days_olist",
            "delivery_days",
        ]
    ].copy()

    dataset = dataset.sort_values(["origin_state", "destination_state", "order_id"])

    training_path = processed_dir / "olist_delivery_training.csv"
    dataset.to_csv(training_path, index=False)

    # =========================
    # Baseline by route
    # =========================
    baseline = (
        dataset.groupby(["origin_state", "destination_state"])
        .agg(
            samples=("delivery_days", "count"),
            min_days=("delivery_days", lambda x: int(x.quantile(0.25))),
            estimated_days=("delivery_days", lambda x: int(x.median())),
            max_days=("delivery_days", lambda x: int(x.quantile(0.75))),
            avg_days=("delivery_days", "mean"),
        )
        .reset_index()
    )

    baseline = baseline[baseline["samples"] >= 5].copy()
    baseline["source"] = "OLIST_BASELINE"
    baseline["model_version"] = "olist-baseline-v1"
    baseline["avg_days"] = baseline["avg_days"].round(2)

    baseline_path = processed_dir / "olist_delivery_route_baseline.csv"
    baseline.to_csv(baseline_path, index=False)

    return training_path, baseline_path


def main():
    parser = argparse.ArgumentParser(
        description="Prepara dataset de entrega a partir da base Olist."
    )
    parser.add_argument(
        "--raw-dir",
        default="data/olist/raw",
        help="Diretório contendo os CSVs brutos da Olist.",
    )
    parser.add_argument(
        "--processed-dir",
        default="data/olist/processed",
        help="Diretório onde os CSVs processados serão salvos.",
    )
    parser.add_argument(
        "--max-delivery-days",
        type=int,
        default=90,
        help="Remove outliers acima desse número de dias.",
    )

    args = parser.parse_args()

    training_path, baseline_path = prepare_delivery_dataset(
        raw_dir=Path(args.raw_dir),
        processed_dir=Path(args.processed_dir),
        max_delivery_days=args.max_delivery_days,
    )

    print(f"Dataset de treino gerado: {training_path}")
    print(f"Baseline de rotas gerado: {baseline_path}")

    training = pd.read_csv(training_path)
    baseline = pd.read_csv(baseline_path)

    print()
    print("Resumo:")
    print(f"- Linhas de treino: {len(training)}")
    print(f"- Rotas baseline: {len(baseline)}")
    print()
    print("Amostra do treino:")
    print(training.head(5).to_string(index=False))
    print()
    print("Amostra do baseline:")
    print(baseline.head(10).to_string(index=False))


if __name__ == "__main__":
    main()
