-- Thêm Admin
INSERT INTO Users (id, username, password, fullName, role)
VALUES ('U001', 'admin', '123', 'Manager A', 'ADMIN');

-- Thêm Bidder
INSERT INTO Users (id, username, password, fullName, role, balance)
VALUES ('U002', 'bidder1', '123', 'Nguyen Van Test', 'BIDDER', 5000.0);

-- Thêm Seller
INSERT INTO Users (id, username, password, fullName, role, storeName)
VALUES ('U003', 'seller1', '123', 'Nguyen Thi Ban Hang', 'SELLER', 'Gom Su Store');