-- used for saving log
CREATE TABLE IF NOT EXISTS log  (
  id int(0) NOT NULL AUTO_INCREMENT,
  type varchar(255) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  message varchar(255) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  creat_time timestamp(0) DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB CHARACTER SET = ascii COLLATE = ascii_general_ci ROW_FORMAT = Dynamic;