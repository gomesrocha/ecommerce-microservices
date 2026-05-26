from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd


BRAZIL_STATES = [
    "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO",
    "MA", "MG", "MS", "MT", "PA", "PB", "PE", "PI", "PR",
    "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO"
]


REQUIRED_FILES = {
    "orders": "olist_orders_dataset.csv",
    "items": "olist_order_items_dataset.csv",
    "customers": "olist_customers_dataset.csv",
    "sellers": "olist_sellers_dataset.csv",
}


def load_csv(raw_dir: Path, filename: str) -> pd.DataFrame:
    path = raw_dir / filename

    if not path.exists():
        raise FileNotFoundError(f"Arquivo não encontrado: {path}")

    return pd.read_csv(path)


def mode_or_unknown(series: pd.Series) -> str:
    values = series.dropna().astype(str).str.upper().str.strip()

    if values.empty:
        return "UNKNOWN"

    return values.mode().iloc[0]


def calculate_route_frequency_score(df: pd.DataFrame) -> pd.Series:
    route_counts = (
        df.groupby(["origin_state", "destination_state"])
        .size()
        .reset_index(name="route_count")
    )

    result = df.merge(route_counts, on=["origin_state", "destination_state"], how="left")

    q25 = result["route_count"].quantile(0.25)

    return (result["route_count"] <= q25).astype(int)


def build_risk_label(dataset: pd.DataFrame) -> pd.DataFrame:
    result = dataset.copy()

    total_amount_p95 = result["total_amount"].quantile(0.95)
    total_amount_p99 = result["total_amount"].quantile(0.99)
    items_quantity_p95 = result["items_quantity"].quantile(0.95)
    max_item_price_p95 = result["max_item_price"].quantile(0.95)

    result["rare_route"] = calculate_route_frequency_score(result)

    result["risk_score"] = 0

    # Valor muito alto em relação à distribuição geral
    result.loc[result["total_amount"] >= total_amount_p95, "risk_score"] += 2
    result.loc[result["total_amount"] >= total_amount_p99, "risk_score"] += 2

    # Muitos itens no pedido
    result.loc[result["items_quantity"] >= items_quantity_p95, "risk_score"] += 1
    result.loc[result["items_quantity"] >= 5, "risk_score"] += 1

    # Produto muito caro
    result.loc[result["max_item_price"] >= max_item_price_p95, "risk_score"] += 2

    # Muitos produtos distintos
    result.loc[result["unique_products"] >= 3, "risk_score"] += 1

    # Rota rara vendedor -> cliente
    result.loc[result["rare_route"] == 1, "risk_score"] += 1

    # Compra interestadual
    result.loc[result["origin_state"] != result["destination_state"], "risk_score"] += 1

    result["label"] = result["risk_score"].apply(
        lambda score: "FRAUD_RISK" if score >= 4 else "LEGIT"
    )

    return result


def one_hot_states(dataset: pd.DataFrame) -> pd.DataFrame:
    output = pd.DataFrame()

    output["total_amount"] = dataset["total_amount"].astype(float)
    output["items_quantity"] = dataset["items_quantity"].astype(float)
    output["avg_item_price"] = dataset["avg_item_price"].astype(float)
    output["max_item_price"] = dataset["max_item_price"].astype(float)
    output["unique_products"] = dataset["unique_products"].astype(float)

    for state in BRAZIL_STATES:
        output[f"origin_{state}"] = (dataset["origin_state"] == state).astype(float)

    for state in BRAZIL_STATES:
        output[f"destination_{state}"] = (dataset["destination_state"] == state).astype(float)

    output["label"] = dataset["label"]

    return output


def prepare_dataset(raw_dir: Path, output_path: Path, max_rows: int | None) -> pd.DataFrame:
    orders = load_csv(raw_dir, REQUIRED_FILES["orders"])
    items = load_csv(raw_dir, REQUIRED_FILES["items"])
    customers = load_csv(raw_dir, REQUIRED_FILES["customers"])
    sellers = load_csv(raw_dir, REQUIRED_FILES["sellers"])

    orders = orders[
        [
            "order_id",
            "customer_id",
            "order_status",
        ]
    ].copy()

    # Usamos somente pedidos entregues para evitar ruído de cancelamentos/logística incompleta.
    orders = orders[orders["order_status"] == "delivered"].copy()

    customers = customers[
        [
            "customer_id",
            "customer_state",
        ]
    ].copy()

    customers = customers.rename(columns={"customer_state": "destination_state"})

    sellers = sellers[
        [
            "seller_id",
            "seller_state",
        ]
    ].copy()

    sellers = sellers.rename(columns={"seller_state": "origin_state"})

    items = items[
        [
            "order_id",
            "order_item_id",
            "product_id",
            "seller_id",
            "price",
        ]
    ].copy()

    item_features = items.merge(sellers, on="seller_id", how="left")

    order_items = (
        item_features.groupby("order_id")
        .agg(
            total_amount=("price", "sum"),
            items_quantity=("order_item_id", "count"),
            avg_item_price=("price", "mean"),
            max_item_price=("price", "max"),
            unique_products=("product_id", "nunique"),
            origin_state=("origin_state", mode_or_unknown),
        )
        .reset_index()
    )

    dataset = (
        orders.merge(customers, on="customer_id", how="inner")
        .merge(order_items, on="order_id", how="inner")
    )

    dataset["origin_state"] = dataset["origin_state"].astype(str).str.upper().str.strip()
    dataset["destination_state"] = dataset["destination_state"].astype(str).str.upper().str.strip()

    dataset = dataset[dataset["origin_state"].isin(BRAZIL_STATES)]
    dataset = dataset[dataset["destination_state"].isin(BRAZIL_STATES)]

    dataset = dataset.dropna(
        subset=[
            "total_amount",
            "items_quantity",
            "avg_item_price",
            "max_item_price",
            "unique_products",
            "origin_state",
            "destination_state",
        ]
    )

    dataset = build_risk_label(dataset)

    # Para evitar dataset muito desbalanceado, mantemos todas as FRAUD_RISK
    # e uma amostra proporcional de LEGIT.
    fraud_risk = dataset[dataset["label"] == "FRAUD_RISK"]
    legit = dataset[dataset["label"] == "LEGIT"]

    if max_rows is not None and len(dataset) > max_rows:
        fraud_target = min(len(fraud_risk), max(1, int(max_rows * 0.35)))
        fraud_sample = fraud_risk.sample(n=fraud_target, random_state=42)

        legit_target = max_rows - len(fraud_sample)
        legit_sample = legit.sample(n=min(len(legit), legit_target), random_state=42)

        dataset = pd.concat([fraud_sample, legit_sample], ignore_index=True)
        dataset = dataset.sample(frac=1.0, random_state=42).reset_index(drop=True)

    tribuo_dataset = one_hot_states(dataset)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    tribuo_dataset.to_csv(output_path, index=False)

    return tribuo_dataset


def main():
    parser = argparse.ArgumentParser(
        description="Prepara dataset simulado de risco/fraude a partir da base Olist."
    )

    parser.add_argument(
        "--raw-dir",
        default="data/olist/raw",
        help="Diretório com os CSVs brutos da Olist."
    )

    parser.add_argument(
        "--output",
        default="data/fraud/olist/processed/olist_fraud_risk_training_tribuo.csv",
        help="Caminho do CSV processado para treino com Tribuo."
    )

    parser.add_argument(
        "--max-rows",
        type=int,
        default=80000,
        help="Número máximo de linhas no dataset final."
    )

    args = parser.parse_args()

    dataset = prepare_dataset(
        raw_dir=Path(args.raw_dir),
        output_path=Path(args.output),
        max_rows=args.max_rows,
    )

    print()
    print(f"Dataset gerado: {args.output}")
    print(f"Linhas: {len(dataset)}")
    print(f"Colunas: {len(dataset.columns)}")
    print()
    print("Distribuição do label:")
    print(dataset["label"].value_counts().to_string())
    print()
    print("Amostra:")
    print(dataset.head(5).to_string(index=False))


if __name__ == "__main__":
    main()
