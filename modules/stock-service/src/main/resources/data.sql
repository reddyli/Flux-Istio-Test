INSERT INTO inventory (sku, quantity) VALUES
                                          ('SKU-1', 100),
                                          ('SKU-2', 50),
                                          ('SKU-3', 0)
ON CONFLICT (sku) DO NOTHING;