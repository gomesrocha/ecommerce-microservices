from __future__ import annotations

import argparse
import math
from pathlib import Path

import pandas as pd


STATES = [
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

# Coordenadas aproximadas das capitais/centros logísticos por UF.
# Uso didático: gerar uma proxy de distância para o modelo.
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


def add_one_hot(row: dict[str, float], prefix: str, value: str, allowed_values: list[str]) -> None:
    for item in allowed_values:
        row[f"{prefix}_{item}"] = 1.0 if value == item else 0.0


def build_features(df: pd.DataFrame) -> pd.DataFrame:
    required_columns = {
        "origin_state",
        "destination_state",
        "items_quantity",
        "delivery_days",
    }

    missing_columns = required_columns - set(df.columns)

    if missing_columns:
        raise ValueError(f"Colunas obrigatórias ausentes: {sorted(missing_columns)}")

    rows: list[dict[str, float]] = []

    for _, source in df.iterrows():
        origin = normalize_state(source["origin_state"])
        destination = normalize_state(source["destination_state"])

        if origin not in STATES or destination not in STATES:
            continue

        items_quantity = float(source["items_quantity"])
        delivery_days = float(source["delivery_days"])

        if delivery_days < 0 or delivery_days > 90:
            continue

        origin_region = REGIONS_BY_STATE[origin]
        destination_region = REGIONS_BY_STATE[destination]

        route_distance_km = haversine_km(origin, destination)
        bucket = distance_bucket(route_distance_km)

        row: dict[str, float] = {}

        row["items_quantity"] = items_quantity
        row["log_items_quantity"] = math.log1p(items_quantity)
        row["same_state"] = 1.0 if origin == destination else 0.0
        row["same_region"] = 1.0 if origin_region == destination_region else 0.0
        row["is_interstate"] = 0.0 if origin == destination else 1.0
        row["route_distance_km"] = route_distance_km

        for distance_bucket_name in ["local", "short", "medium", "long", "very_long"]:
            row[f"distance_{distance_bucket_name}"] = 1.0 if bucket == distance_bucket_name else 0.0

        add_one_hot(row, "origin_region", origin_region, REGIONS)
        add_one_hot(row, "destination_region", destination_region, REGIONS)

        add_one_hot(row, "origin", origin, STATES)
        add_one_hot(row, "destination", destination, STATES)

        row["delivery_days"] = delivery_days

        rows.append(row)

    return pd.DataFrame(rows)


def main() -> None:
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--input",
        default="data/olist/processed/olist_delivery_training.csv",
        help="Arquivo CSV processado com as colunas origin_state, destination_state, items_quantity e delivery_days.",
    )

    parser.add_argument(
        "--output",
        default="delivery-estimator-api/src/main/resources/ml/olist_delivery_training_tribuo.csv",
        help="Arquivo CSV de saída no formato esperado pelo Tribuo.",
    )

    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        raise FileNotFoundError(f"Arquivo de entrada não encontrado: {input_path}")

    df = pd.read_csv(input_path)

    output = build_features(df)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output.to_csv(output_path, index=False)

    print(f"CSV Tribuo gerado em: {output_path}")
    print(f"Linhas: {len(output)}")
    print(f"Colunas: {len(output.columns)}")
    print("Colunas:")
    print(", ".join(output.columns))


if __name__ == "__main__":
    main()
