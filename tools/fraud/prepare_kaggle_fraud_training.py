from __future__ import annotations

import argparse
import math
from pathlib import Path

import pandas as pd


TARGET_COLUMN = "label"

REQUIRED_COLUMNS = {
    "trans_date_trans_time",
    "amt",
    "is_fraud",
}


def amount_bucket(total_amount: float) -> str:
    if total_amount <= 50:
        return "low"

    if total_amount <= 200:
        return "medium"

    if total_amount <= 500:
        return "high"

    return "very_high"


def hour_bucket(hour: int) -> str:
    if 0 <= hour <= 5:
        return "night"

    if 6 <= hour <= 11:
        return "morning"

    if 12 <= hour <= 17:
        return "afternoon"

    return "evening"


def safe_divide(numerator: float, denominator: float) -> float:
    if denominator == 0:
        return 0.0

    return numerator / denominator


def validate_columns(df: pd.DataFrame, source: Path) -> None:
    missing = REQUIRED_COLUMNS - set(df.columns)

    if missing:
        raise ValueError(f"Arquivo {source} não possui colunas obrigatórias: {sorted(missing)}")


def read_kaggle_files(raw_dir: Path) -> pd.DataFrame:
    train_path = raw_dir / "fraudTrain.csv"
    test_path = raw_dir / "fraudTest.csv"

    if not train_path.exists():
        raise FileNotFoundError(f"Arquivo não encontrado: {train_path}")

    if not test_path.exists():
        raise FileNotFoundError(f"Arquivo não encontrado: {test_path}")

    train_df = pd.read_csv(train_path)
    test_df = pd.read_csv(test_path)

    validate_columns(train_df, train_path)
    validate_columns(test_df, test_path)

    dataset = pd.concat([train_df, test_df], ignore_index=True)

    return dataset


def build_features(dataset: pd.DataFrame) -> pd.DataFrame:
    result = dataset.copy()

    result["transaction_datetime"] = pd.to_datetime(
        result["trans_date_trans_time"],
        errors="coerce"
    )

    result = result.dropna(subset=["transaction_datetime", "amt", "is_fraud"])

    result["total_amount"] = result["amt"].astype(float)
    result["log_total_amount"] = result["total_amount"].apply(math.log1p)

    # O evento OrderCreatedEvent não possui os mesmos dados do Kaggle.
    # Por isso, geramos um schema canônico compatível com o runtime atual.
    result["items_quantity"] = 1.0
    result["log_items_quantity"] = math.log1p(1.0)

    result["avg_item_price"] = result["total_amount"]
    result["log_avg_item_price"] = result["avg_item_price"].apply(math.log1p)

    result["max_item_price"] = result["total_amount"]
    result["log_max_item_price"] = result["max_item_price"].apply(math.log1p)

    result["unique_products"] = 1.0
    result["amount_per_item"] = result["total_amount"]
    result["price_spread"] = 0.0
    result["max_to_avg_price_ratio"] = 1.0

    result["transaction_hour"] = result["transaction_datetime"].dt.hour.astype(float)
    result["transaction_day_of_week"] = result["transaction_datetime"].dt.dayofweek.astype(float)
    result["transaction_is_weekend"] = result["transaction_day_of_week"].isin([5, 6]).astype(float)

    result["amount_bucket"] = result["total_amount"].apply(amount_bucket)
    result["hour_bucket"] = result["transaction_hour"].astype(int).apply(hour_bucket)

    for bucket in ["low", "medium", "high", "very_high"]:
        result[f"amount_{bucket}"] = (result["amount_bucket"] == bucket).astype(float)

    result["items_single"] = 1.0
    result["items_few"] = 0.0
    result["items_many"] = 0.0

    for bucket in ["night", "morning", "afternoon", "evening"]:
        result[f"hour_{bucket}"] = (result["hour_bucket"] == bucket).astype(float)

    result[TARGET_COLUMN] = result["is_fraud"].apply(
        lambda value: "FRAUD_RISK" if int(value) == 1 else "LEGIT"
    )

    columns = [
        "total_amount",
        "log_total_amount",
        "items_quantity",
        "log_items_quantity",
        "avg_item_price",
        "log_avg_item_price",
        "max_item_price",
        "log_max_item_price",
        "unique_products",
        "amount_per_item",
        "price_spread",
        "max_to_avg_price_ratio",
        "transaction_hour",
        "transaction_day_of_week",
        "transaction_is_weekend",
        "amount_low",
        "amount_medium",
        "amount_high",
        "amount_very_high",
        "items_single",
        "items_few",
        "items_many",
        "hour_night",
        "hour_morning",
        "hour_afternoon",
        "hour_evening",
        TARGET_COLUMN,
    ]

    return result[columns]


def balance_dataset(dataset: pd.DataFrame, legit_multiplier: int, max_rows: int | None) -> pd.DataFrame:
    fraud = dataset[dataset[TARGET_COLUMN] == "FRAUD_RISK"]
    legit = dataset[dataset[TARGET_COLUMN] == "LEGIT"]

    if fraud.empty:
        raise ValueError("Dataset não possui registros FRAUD_RISK.")

    target_legit = min(len(legit), len(fraud) * legit_multiplier)

    if max_rows is not None:
        max_fraud = max(1, max_rows // (legit_multiplier + 1))
        fraud = fraud.sample(n=min(len(fraud), max_fraud), random_state=42)
        target_legit = min(len(legit), max_rows - len(fraud), len(fraud) * legit_multiplier)

    legit_sample = legit.sample(n=target_legit, random_state=42)

    balanced = pd.concat([fraud, legit_sample], ignore_index=True)
    balanced = balanced.sample(frac=1.0, random_state=42).reset_index(drop=True)

    return balanced


def prepare(raw_dir: Path, output: Path, max_rows: int | None, legit_multiplier: int) -> pd.DataFrame:
    raw_dataset = read_kaggle_files(raw_dir)
    features = build_features(raw_dataset)
    balanced = balance_dataset(features, legit_multiplier=legit_multiplier, max_rows=max_rows)

    output.parent.mkdir(parents=True, exist_ok=True)
    balanced.to_csv(output, index=False)

    return balanced


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Prepara dataset Kaggle de fraude para treino Tribuo."
    )

    parser.add_argument(
        "--raw-dir",
        default="data/fraud/kaggle/raw",
        help="Diretório com fraudTrain.csv e fraudTest.csv."
    )

    parser.add_argument(
        "--output",
        default="data/fraud/kaggle/processed/kaggle_fraud_training_tribuo.csv",
        help="Caminho de saída do CSV processado."
    )

    parser.add_argument(
        "--max-rows",
        type=int,
        default=120000,
        help="Máximo de linhas no dataset final balanceado."
    )

    parser.add_argument(
        "--legit-multiplier",
        type=int,
        default=4,
        help="Quantidade de registros LEGIT por registro FRAUD_RISK."
    )

    args = parser.parse_args()

    dataset = prepare(
        raw_dir=Path(args.raw_dir),
        output=Path(args.output),
        max_rows=args.max_rows,
        legit_multiplier=args.legit_multiplier,
    )

    print()
    print(f"Dataset Kaggle Tribuo gerado em: {args.output}")
    print(f"Linhas: {len(dataset)}")
    print(f"Colunas: {len(dataset.columns)}")
    print()
    print("Distribuição do label:")
    print(dataset[TARGET_COLUMN].value_counts().to_string())
    print()
    print("Colunas:")
    print(", ".join(dataset.columns))
    print()
    print("Amostra:")
    print(dataset.head(5).to_string(index=False))


if __name__ == "__main__":
    main()
