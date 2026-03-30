CREATE TABLE risk_check_result (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    rule_name VARCHAR(64) NOT NULL,
    passed BOOLEAN NOT NULL,
    message VARCHAR(255) NOT NULL,
    checked_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_risk_check_result_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_risk_check_result_order_id
    ON risk_check_result(order_id);
