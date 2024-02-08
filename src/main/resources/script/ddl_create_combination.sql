-- used for save result for java algorithm
CREATE TABLE IF NOT EXISTS combination  (
  id int(0) NOT NULL AUTO_INCREMENT,
  batch_id int(0) NOT NULL,
  combine_number int(0) NOT NULL,
  combine_cost int(0) NOT NULL,
  path text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  path_cost int(0) NOT NULL,
  shared_path text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  shared_path_cost int(0) NOT NULL,
  created_by varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  created_time timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = 'record the final result' ROW_FORMAT = Dynamic;