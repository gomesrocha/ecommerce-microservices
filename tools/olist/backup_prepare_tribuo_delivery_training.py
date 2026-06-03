from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd


BRAZIL_STATES = [
    "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO",
    "MA", "MG", "MS", "MT", "PA", "PB", "PE", "PI", "PR",
    "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO"
]


def main():
    parser = argparse.ArgumentParser(
        description="Gera CSV numérico para treino Tribuo a partir do dataset Olist tratado."
    )
    parser.add_argument(
        "--input",
        default="data/olist/processed/olist_delivery_training.csv",
        help="CSV tratado gerado pelo ETL Olist."
    )
    parser.add_argument(
        "--output",
        default="delivery-estimator-api/src/main/resources/ml/olist_delivery_training_tribuo.csv",
        help="CSV numérico para treino Tribuo."
    )

    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        raise FileNotFoundError(f"Arquivo não encontrado: {input_path}")

    df = pd.read_csv(input_path)

    required = [
        "origin_state",
        "destination_state",
        "items_quantity",
        "delivery_days",
    ]

    missing = [column for column in required if column not in df.columns]
    if missing:
        raise ValueError(f"Colunas obrigatórias ausentes: {missing}")

    df = df[required].copy()

    df["origin_state"] = df["origin_state"].astype(str).str.upper().str.strip()
    df["destination_state"] = df["destination_state"].astype(str).str.upper().str.strip()

    df = df[df["origin_state"].isin(BRAZIL_STATES)]
    df = df[df["destination_state"].isin(BRAZIL_STATES)]

    df["items_quantity"] = pd.to_numeric(df["items_quantity"], errors="coerce").fillna(1)
    df["delivery_days"] = pd.to_numeric(df["delivery_days"], errors="coerce")

    df = df.dropna(subset=["delivery_days"])
    df = df[(df["delivery_days"] >= 0) & (df["delivery_days"] <= 90)]

    output = pd.DataFrame()
    output["items_quantity"] = df["items_quantity"].astype(float)

    for state in BRAZIL_STATES:
        output[f"origin_{state}"] = (df["origin_state"] == state).astype(float)

    for state in BRAZIL_STATES:
        output[f"destination_{state}"] = (df["destination_state"] == state).astype(float)

    output["delivery_days"] = df["delivery_days"].astype(float)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output.to_csv(output_path, index=False)

    print(f"CSV Tribuo gerado: {output_path}")
    print(f"Linhas: {len(output)}")
    print(f"Colunas: {len(output.columns)}")
    print(output.head().to_string(index=False))


if __name__ == "__main__":
    main()
