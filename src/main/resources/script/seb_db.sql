SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `sep_db`
--

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `AddConnection` (IN `source_device_id` INT, IN `destination_device_id` INT, IN `cost` DECIMAL(10,2))  BEGIN
    INSERT INTO connections (source_device, destination_device, cost)
    VALUES (source_device_id, destination_device_id, cost);
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `AddDevice` (IN `device_name` VARCHAR(255), IN `device_type` VARCHAR(50), IN `is_source` INT, IN `is_destination` INT)  BEGIN
    INSERT INTO devices (name, type, is_source, is_destination) VALUES (device_name, device_type, is_source, is_destination);
    
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `DeleteConnection` (IN `source_device_id` INT(11), IN `destination_device_id` INT(11))  NO SQL
BEGIN
    DELETE FROM connections 
    WHERE source_device = source_device_id 
        AND destination_device = destination_device_id;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `DeleteDevice` (IN `device_id` INT(11))  NO SQL
BEGIN
    -- 1. delete connections associated with the device
    DELETE FROM connections 
    WHERE source_device = device_id OR destination_device = device_id;
    
    -- 2. delete the device 
    DELETE FROM devices 
    WHERE id = device_id;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `FindAllPaths` (`source_id` INT, `destination_id` INT)  BEGIN
    DECLARE max_depth INT DEFAULT 10; -- Maximum depth of traversal

    -- Create a table to store paths
    CREATE TABLE paths (
        id INT AUTO_INCREMENT PRIMARY KEY,
        depth INT,
        nodes TEXT
    );

    -- Insert the initial path (source device) into the table
    INSERT INTO paths (depth, nodes)
    SELECT 0, CAST(source_id AS CHAR);

    -- Loop to find all paths using depth-first traversal
    WHILE max_depth > 0 DO
        -- Find the path with the current depth from the paths table
        SET @current_path = (SELECT nodes FROM paths WHERE depth = max_depth - 1);

        -- Find the last device in the current path
        SET @last_device = CAST(SUBSTRING_INDEX(@current_path, '->', -1) AS UNSIGNED);

        -- Insert new paths into the table
        INSERT INTO paths (depth, nodes)
        SELECT max_depth, CONCAT(@current_path, '->', c.destination_device)
        FROM connections AS c
        WHERE c.source_device = @last_device
              AND NOT EXISTS (SELECT 1 FROM paths WHERE nodes = CONCAT(@current_path, '->', c.destination_device));

        SET max_depth = max_depth - 1;
    END WHILE;

    -- Return all paths
    SELECT nodes FROM paths
    WHERE nodes LIKE CONCAT(source_id, '%->', destination_id)
    ORDER BY nodes;

    -- Drop the paths table
    DROP TABLE IF EXISTS paths;

END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `UpdateConnection` (IN `new_cost` INT(11), IN `source_device_id` INT(11), IN `destination_device_id` INT(11))  NO SQL
BEGIN
    UPDATE connections 
    SET cost = new_cost
    WHERE source_device = source_device_id 
        AND destination_device = destination_device_id;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `UpdateDevice` (IN `device_id` INT(11), IN `new_device_name` VARCHAR(255), IN `new_device_type` VARCHAR(20), IN `new_is_source` INT(11), IN `new_is_destination` INT(11))  NO SQL
BEGIN
    UPDATE devices 
    SET name = new_device_name, 
        type = new_device_type, 
        is_source = new_is_source, 
        is_destination = new_is_destination
    WHERE id = device_id;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `connections`
--

CREATE TABLE `connections` (
  `id` int(11) NOT NULL,
  `source_device` int(11) DEFAULT NULL,
  `destination_device` int(11) DEFAULT NULL,
  `cost` int(11) DEFAULT '1'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `connections`
--

INSERT INTO `connections` (`id`, `source_device`, `destination_device`, `cost`) VALUES
(1, 1, 2, 5),
(2, 2, 3, 3),
(3, 2, 4, 2),
(4, 3, 4, 1),
(9, 6, 7, 2),
(10, 1, 3, 9);

-- --------------------------------------------------------

--
-- Table structure for table `devices`
--

CREATE TABLE `devices` (
  `id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `in_use` int(11) DEFAULT NULL,
  `is_source` int(11) NOT NULL,
  `is_destination` int(11) NOT NULL,
  `cost` int(11) NOT NULL DEFAULT '1'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dumping data for table `devices`
--

INSERT INTO `devices` (`id`, `name`, `type`, `status`, `in_use`, `is_source`, `is_destination`, `cost`) VALUES
(1, 'tes', 'tess', 'Operational', 0, 1, 0, 0),
(2, 'Device B', 'Intermediate', 'Operational', 0, 0, 0, 0),
(3, 'Device C', 'Convergent', 'Operational', 0, 0, 0, 0),
(4, 'Device D', 'Intermediate', 'Faulty', 1, 0, 0, 0),
(6, 'Device F', 'Divergent', 'Operational', 1, 0, 0, 0),
(7, 'Device G', 'Destination', 'Operational', 0, 0, 0, 0),
(8, 'd1', 't1', NULL, NULL, 1, 0, 1),
(9, 'SWITCH_1', 'divertor', NULL, NULL, 0, 0, 1);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `connections`
--
ALTER TABLE `connections`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `devices`
--
ALTER TABLE `devices`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `connections`
--
ALTER TABLE `connections`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `devices`
--
ALTER TABLE `devices`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;
