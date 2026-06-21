-- Export the Database
-- Go to C:\xampp\mysql\bin
mysqldump -u root -p money_transfer_db > money_transfer_backup.sql

-- Import the Database
-- DB must exist
mysql -u root -p money_transfer_db < money_transfer_backup.sql