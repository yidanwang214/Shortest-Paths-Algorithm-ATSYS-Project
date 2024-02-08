-- used for the path results
CREATE TABLE IF NOT EXISTS path  (
  id int(0) NOT NULL AUTO_INCREMENT,
  batch_id int(0) NOT NULL,
  source int(0) NOT NULL,
  target int(0) NOT NULL,
  total_node int(0) NOT NULL,
  total_cost int(0) NOT NULL,
  path text CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  created_by varchar(20) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  created_time timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB CHARACTER SET = ascii COLLATE = ascii_general_ci ROW_FORMAT = Dynamic;