ALTER TABLE devices
-- add column
ADD COLUMN cost int(0) NOT NULL DEFAULT 0 AFTER type;
-- Construct optimize: set not null, use enum
MODIFY COLUMN name varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
MODIFY COLUMN type enum('Source','Convergent','Intermediate','Divergent','Destination') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
MODIFY COLUMN status enum('Faulty','Operational') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL;


-- Construct optimize: set not null,
ALTER TABLE connections
MODIFY COLUMN source_device int(0) NOT NULL,
MODIFY COLUMN destination_device int(0) NOT NULL,
MODIFY COLUMN weight int(0) NOT NULL;


-- Add indexes
ALTER TABLE devices ADD UNIQUE INDEX uk_name(name);
ALTER TABLE connections
ADD INDEX idx_source_weight(source_device, weight),
ADD INDEX idx_destination_weight(destination_device, weight),
ADD UNIQUE INDEX uk_source_destination(source_device, destination_device);