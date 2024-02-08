-- used for algorithm
CREATE TABLE IF NOT EXISTS temp_path  (
  id int(0) NOT NULL AUTO_INCREMENT,
  current_node_id int(0) NOT NULL,
  type varchar(10) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  comulative_cost int(0) NOT NULL,
  depth int(0) NOT NULL,
  node_ids varchar(255) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  node_names varchar(1000) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  finish int(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (id) USING BTREE,
  INDEX idx_node_id(current_node_id) USING BTREE
) ENGINE = MEMORY CHARACTER SET = ascii COLLATE = ascii_general_ci ROW_FORMAT = Dynamic;