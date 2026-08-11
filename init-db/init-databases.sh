#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE orderdb;
    CREATE DATABASE inventorydb;
    CREATE DATABASE inventorysagadb;
    CREATE DATABASE paymentdb;
EOSQL