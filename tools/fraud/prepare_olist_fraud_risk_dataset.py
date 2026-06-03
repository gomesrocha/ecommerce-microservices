from __future__ import annotations

import argparse
import math
from pathlib import Path

import pandas as pd


BRAZIL_STATES = [
    "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO",
    "MA", "MG", "MS", "MT", "PA", "PB", "PE", "PI", "PR",
    "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO"
]

REGIONS_BY_STATE = {
    "AC": "NORTE",
    "AP": "NORTE",
    "AM": "NORTE",
    "PA": "NORTE",
    "RO": "NORTE",
    "RR": "NORTE",
    "TO": "NORTE",

    "AL": "NORDESTE",
    "BA": "NORDESTE",
    "CE": "NORDESTE",
    "MA": "NORDESTE",
    "PB": "NORDESTE",
    "PE": "NORDESTE",
    "PI": "NORDESTE",
    "RN": "NORDESTE",
    "SE": "NORDESTE",

    "DF": "CENTRO_OESTE",
    "GO": "CENTRO_OESTE",
    "MT": "CENTRO_OESTE",
    "MS": "CENTRO_OESTE",

    "ES": "SUDESTE",
    "MG": "SUDESTE",
    "RJ": "SUDESTE",
    "SP": "SUDESTE",

    "PR": "SUL",
    "RS": "SUL",
    "SC": "SUL",
}

REGIONS = [
    "NORTE",
    "NORDESTE",
    "CENTRO_OESTE",
    "SUDESTE",
    "SUL",
]

STATE_COORDS = {
    "AC": (-9.97, -67.81),
    "AL": (-9.66, -35.73),
    "AM": (-3.10, -60.02),
    "AP": (0.03, -51.05),
    "BA": (-12.97, -38.50),
    "CE": (-3.73, -38.52),
    "DF": (-15.79, -47.88),
    "ES": (-20.31, -40.31),
    "GO": (-16.68, -49.25),
    "MA": (-2.53, -44.30),
    "MG": (-19.92, -43.94),
    "MS": (-20.45, -54.62),
    "MT": (-15.60, -56.10),
    "PA": (-1.45, -48.50),
    "PB": (-7.12, -34.86),
    "PE": (-8.05, -34.90),
    "PI": (-5.09, -42.80),
    "PR": (-25.43, -49.27),
    "RJ": (-22.91, -43.17),
    "RN": (-5.79, -35.21),
    "RO": (-8.76, -63.90),
    "RR": (2.82, -60.67),
    "RS": (-30.03, -51.23),
    "SC": (-27.59, -48.55),
    "SE": (-10.91, -37.07),
    "SP": (-23.55, -46.63),
    "TO": (-10.18, -48.33),
}

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


def normalize_state(value: object) -> str:
    if pd.isna(value):
        return ""

    return str(value).strip().upper()


def haversine_km(origin: str, destination: str) -> float:
    if origin not in STATE_COORDS or destination not in STATE_COORDS:
        return 0.0

    lat1, lon1 = STATE_COORDS[origin]
    lat2, lon2 = STATE_COORDS[destination]

    radius_km = 6371.0

    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)

    a = (
        math.sin(delta_phi / 2.0) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2.0) ** 2
    )

    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

    return round(radius_km * c, 2)


def distance_bucket(distance_km: float) -> str:
    if distance_km <= 150:
        return "local"

    if distance_km <= 600:
        return "short"

    if distance_km <= 1200:
        return "medium"

    if distance_km <= 2200:
        return "long"

    return "very_long"


def amount_bucket(total_amount: float) -> str:
    if total_amount <= 100:
        return "low"

    if total_amount <= 500:
        return "medium"

    if total_amount <= 1000:
        return "high"

    return "very_high"


def items_bucket(items_quantity: float) -> str:
    if items_quantity <= 1:
        return "single"

    if items_quantity <= 3:
        return "few"

    return "many"


def safe_divide(numerator: float, denominator: float) -> float:
    if denominator == 0:
        return 0.0

    return numerator / denominator


def calculate_route_frequency_score(df: pd.DataFrame) -> pd.Series:
    route_counts = (
        df.groupby(["origin_state", "destination_state"])
        .size()
        .reset_index(name="route_count")
    )

    result = df.merge(route_counts, on=["origin_state", "destination_state"], how="left")

    q25 = result["route_count"].quantile(0.25)

    return (result["route_count"] <= q25).astype(int)


def enrich_route_features(dataset: pd.DataFrame) -> pd.DataFrame:
    result = dataset.copy()

    result["origin_region"] = result["origin_state"].map(REGIONS_BY_STATE)
    result["destination_region"] = result["destination_state"].map(REGIONS_BY_STATE)

    result["same_state"] = (result["origin_state"] == result["destination_state"]).astype(int)
    result["same_region"] = (result["origin_region"] == result["destination_region"]).astype(int)
    result["is_interstate"] = (result["origin_state"] != result["destination_state"]).astype(int)

    result["route_distance_km"] = result.apply(
        lambda row: haversine_km(row["origin_state"], row["destination_state"]),
        axis=1,
    )

    result["distance_bucket"] = result["route_distance_km"].apply(distance_bucket)

    return result


def build_risk_label(dataset: pd.DataFrame) -> pd.DataFrame:
    result = dataset.copy()

    total_amount_p95 = result["total_amount"].quantile(0.95)
    total_amount_p99 = result["total_amount"].quantile(0.99)
    items_quantity_p95 = result["items_quantity"].quantile(0.95)
    max_item_price_p95 = result["max_item_price"].quantile(0.95)

    result["rare_route"] = calculate_route_frequency_score(result)

    result["risk_score"] = 0

    result.loc[result["total_amount"] >= total_amount_p95, "risk_score"] += 2
    result.loc[result["total_amount"] >= total_amount_p99, "risk_score"] += 2

    result.loc[result["items_quantity"] >= items_quantity_p95, "risk_score"] += 1
    result.loc[result["items_quantity"] >= 5, "risk_score"] += 1

    result.loc[result["max_item_price"] >= max_item_price_p95, "risk_score"] += 2

    result.loc[result["unique_products"] >= 3, "risk_score"] += 1

    result.loc[result["rare_route"] == 1, "risk_score"] += 1

    result.loc[result["is_interstate"] == 1, "risk_score"] += 1
    result.loc[result["same_region"] == 0, "risk_score"] += 1
    result.loc[result["route_distance_km"] >= 2200, "risk_score"] += 1

    result.loc[
        (result["total_amount"] >= total_amount_p95)
        & (result["is_interstate"] == 1),
        "risk_score"
    ] += 1

    result["label"] = result["risk_score"].apply(
        lambda score: "FRAUD_RISK" if score >= 4 else "LEGIT"
    )

    return result


def add_one_hot(row: dict[str, float], prefix: str, value: str, allowed_values: list[str]) -> None:
    for item in allowed_values:
        row[f"{prefix}_{item}"] = 1.0 if value == item else 0.0


def build_tribuo_features(dataset: pd.DataFrame) -> pd.DataFrame:
    rows: list[dict[str, float | str]] = []

    for _, source in dataset.iterrows():
        origin = normalize_state(source["origin_state"])
        destination = normalize_state(source["destination_state"])

        if origin not in BRAZIL_STATES or destination not in BRAZIL_STATES:
            continue

        total_amount = float(source["total_amount"])
        items_quantity = float(source["items_quantity"])
        avg_item_price = float(source["avg_item_price"])
        max_item_price = float(source["max_item_price"])
        unique_products = float(source["unique_products"])

        origin_region = REGIONS_BY_STATE[origin]
        destination_region = REGIONS_BY_STATE[destination]

        route_distance_km = haversine_km(origin, destination)
        route_distance_bucket = distance_bucket(route_distance_km)

        amount_bucket_name = amount_bucket(total_amount)
        items_bucket_name = items_bucket(items_quantity)

        amount_per_item = safe_divide(total_amount, items_quantity)
        price_spread = max_item_price - avg_item_price
        max_to_avg_price_ratio = safe_divide(max_item_price, avg_item_price)

        row: dict[str, float | str] = {}

        row["total_amount"] = total_amount
        row["log_total_amount"] = math.log1p(total_amount)
        row["items_quantity"] = items_quantity
        row["log_items_quantity"] = math.log1p(items_quantity)
        row["avg_item_price"] = avg_item_price
        row["log_avg_item_price"] = math.log1p(avg_item_price)
        row["max_item_price"] = max_item_price
        row["log_max_item_price"] = math.log1p(max_item_price)
        row["unique_products"] = unique_products
        row["amount_per_item"] = amount_per_item
        row["price_spread"] = price_spread
        row["max_to_avg_price_ratio"] = max_to_avg_price_ratio

        row["same_state"] = 1.0 if origin == destination else 0.0
        row["same_region"] = 1.0 if origin_region == destination_region else 0.0
        row["is_interstate"] = 0.0 if origin == destination else 1.0
        row["route_distance_km"] = route_distance_km

        for bucket in ["local", "short", "medium", "long", "very_long"]:
            row[f"distance_{bucket}"] = 1.0 if route_distance_bucket == bucket else 0.0

        for bucket in ["low", "medium", "high", "very_high"]:
            row[f"amount_{bucket}"] = 1.0 if amount_bucket_name == bucket else 0.0

        for bucket in ["single", "few", "many"]:
            row[f"items_{bucket}"] = 1.0 if items_bucket_name == bucket else 0.0

        add_one_hot(row, "origin_region", origin_region, REGIONS)
        add_one_hot(row, "destination_region", destination_region, REGIONS)

        add_one_hot(row, "origin", origin, BRAZIL_STATES)
        add_one_hot(row, "destination", destination, BRAZIL_STATES)

        row["label"] = source["label"]

        rows.append(row)

    return pd.DataFrame(rows)


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

    dataset = enrich_route_features(dataset)
    dataset = build_risk_label(dataset)

    fraud_risk = dataset[dataset["label"] == "FRAUD_RISK"]
    legit = dataset[dataset["label"] == "LEGIT"]

    if max_rows is not None and len(dataset) > max_rows:
        fraud_target = min(len(fraud_risk), max(1, int(max_rows * 0.35)))
        fraud_sample = fraud_risk.sample(n=fraud_target, random_state=42)

        legit_target = max_rows - len(fraud_sample)
        legit_sample = legit.sample(n=min(len(legit), legit_target), random_state=42)

        dataset = pd.concat([fraud_sample, legit_sample], ignore_index=True)
        dataset = dataset.sample(frac=1.0, random_state=42).reset_index(drop=True)

    tribuo_dataset = build_tribuo_features(dataset)

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
    print("Colunas:")
    print(", ".join(dataset.columns))
    print()
    print("Amostra:")
    print(dataset.head(5).to_string(index=False))


if __name__ == "__main__":
    main()
