import os
import uuid
import json
from dotenv import load_dotenv
from datetime import datetime, timezone

from google.oauth2 import service_account
from google.cloud import pubsub_v1

load_dotenv()

# Set PubSub Project Details
PROJECT_ID = os.getenv("GCP_PROJECT_ID") or ""
TOPIC_ID = os.getenv("GCP_TOPIC_ID") or ""

# Path to service account key JSON file
SERVICE_ACCOUNT_FILE = f"resources/{os.getenv("GCP_SERVICE_ACCOUNT_FILE")}"

def build_order_placed_event():
    return {
        "eventType": "ORDER_PLACED",
        "eventVersion": "1.0",
        "eventId": str(uuid.uuid4()),
        "eventTime": datetime.now(timezone.utc).isoformat(),
        "source": "order-service",
        "data": {
            "orderId": str(uuid.uuid4()).replace("-", "")[0:8],
            "orderDate": datetime.now(timezone.utc).isoformat(),
            "orderStatus": "PLACED",
            "customer": {
                "customerId": "CUST-98765",
                "email": "customer@example.com"
            },
            "items": [
                {
                    "sku": "SKU-IPHONE-14",
                    "name": "iPhone 14",
                    "quantity": 1
                },
                {
                    "sku": "SKU-CASE-BLACK",
                    "name": "Phone Case - Black",
                    "quantity": 2
                }
            ],
            "shipping": {
                "address": {
                    "name": "John Doe",
                    "line1": "12 MG Road",
                    "line2": None,
                    "city": "Bengaluru",
                    "state": "KA",
                    "postalCode": "560038",
                    "country": "IN",
                    "phone": "+91-9876543210"
                },
                "method": "STANDARD"
            },
            "payment": {
                "paymentMode": "CARD",
                "paymentStatus": "PAID"
            },
            "totalAmount": {
                "currency": "INR",
                "value": 129999.00
            }
        }
    }

def publish_message():
    # Authenticate using service account
    credentials = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE, 
        scopes=["https://www.googleapis.com/auth/cloud-platform"])
    publisher = pubsub_v1.PublisherClient(credentials=credentials)
    topic_path = publisher.topic_path(PROJECT_ID, TOPIC_ID)

    event = build_order_placed_event()

    # Pub/Sub expects bytes
    data = json.dumps(event).encode("utf-8")

    future = publisher.publish(
        topic=topic_path,
        data=data,
        event_type="ORDER_PLACED",
        eventVersion="1.0",
        source="order-service"
    )

    message_id = future.result()
    print(f"Message published successfully. Message ID: {message_id}")



if __name__ == "__main__":
    publish_message()
