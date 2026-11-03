CREATE TABLE finance.client (
 c_id INTEGER PRIMARY KEY,
 c_name VARCHAR(100) NOT NULL,
 c_id_card CHAR(20) UNIQUE NOT NULL,
 c_phone CHAR(20) UNIQUE NOT NULL,
 c_mail CHAR(30) UNIQUE
);
CREATE TABLE finance.bank_card (
 b_number CHAR(30) PRIMARY KEY,
 b_type CHAR(20) NOT NULL,
 b_client_id INTEGER NOT NULL REFERENCES finance.client(c_id)
);
CREATE TABLE finance.financial_product (
 p_id INTEGER PRIMARY KEY,
 p_name VARCHAR(100) NOT NULL,
 p_description VARCHAR(1000),
 p_amount INTEGER CHECK (p_amount >= 0),
 p_year INTEGER CHECK (p_year > 0)
);
CREATE TABLE finance.financial_asset (
 a_id INTEGER PRIMARY KEY,
 a_client_id INTEGER NOT NULL REFERENCES finance.client(c_id),
 a_product_id INTEGER NOT NULL REFERENCES finance.financial_product(p_id),
 a_type INTEGER NOT NULL CHECK (a_type=1),
 a_status CHAR(20) NOT NULL CHECK (a_status IN ('可用','冻结')),
 a_quantity INTEGER NOT NULL CHECK (a_quantity>0),
 a_income INTEGER NOT NULL CHECK (a_income>=0),
 a_purchase_time DATE NOT NULL
);
CREATE TABLE finance.command_request (
 request_id VARCHAR(80) PRIMARY KEY,
 action VARCHAR(16) NOT NULL CHECK (action IN ('BUY','REDEEM')),
 payload_hash CHAR(64) NOT NULL,
 result_id INTEGER,
 result_code VARCHAR(16) NOT NULL CHECK (result_code IN ('PENDING','SUCCESS'))
);
CREATE TABLE finance.transaction_event (
 request_id VARCHAR(80) PRIMARY KEY REFERENCES finance.command_request(request_id),
 action VARCHAR(16) NOT NULL,
 asset_id INTEGER NOT NULL,
 client_id INTEGER NOT NULL,
 product_id INTEGER NOT NULL,
 quantity INTEGER NOT NULL CHECK (quantity>0),
 income INTEGER NOT NULL CHECK (income>=0),
 event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_card_client ON finance.bank_card(b_client_id);
CREATE INDEX idx_asset_client ON finance.financial_asset(a_client_id);
CREATE INDEX idx_asset_product ON finance.financial_asset(a_product_id);
CREATE INDEX idx_asset_purchase ON finance.financial_asset(a_purchase_time);
