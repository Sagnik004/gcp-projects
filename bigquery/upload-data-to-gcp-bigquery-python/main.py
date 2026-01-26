import os
from dotenv import load_dotenv
import pandas as pd
from pandas_gbq import to_gbq
from google.oauth2 import service_account

load_dotenv()

# Set BigQuery Project Details, input file path, and destination table
PROJECT_ID = os.getenv("GCP_PROJECT_ID")
DATASET_ID = os.getenv("GCP_BQ_DATASET_ID")
TABLE_ID = os.getenv("GCP_BQ_TABLE_ID")
CSV_PATH = "resources/Sales_Records_50K.csv"  # Path to CSV file
DESTINATION_TABLE = f"{DATASET_ID}.{TABLE_ID}"

# Path to service account key JSON file
SERVICE_ACCOUNT_FILE = f"resources/{os.getenv("GCP_SERVICE_ACCOUNT_FILE")}"

# Main process
def main():
    # Load csv file
    df = pd.read_csv(CSV_PATH)
    
    # Optional: Clean column names (recommended for BigQuery)
    df.columns = (
        df.columns
          .str.strip()
          .str.lower()
          .str.replace(" ", "_")
    )

    # Convert date columns
    df["order_date"] = pd.to_datetime(df["order_date"])
    df["ship_date"] = pd.to_datetime(df["ship_date"])

    # Authenticate using service account
    credentials = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE,
        scopes=["https://www.googleapis.com/auth/cloud-platform"]
    )

    # Upload to BigQuery
    to_gbq(
        dataframe=df,
        destination_table=DESTINATION_TABLE,
        project_id=PROJECT_ID,
        credentials=credentials,
        if_exists="replace"
    )
    print("✅ CSV data successfully uploaded to BigQuery!")


if __name__ == "__main__":
    main()
