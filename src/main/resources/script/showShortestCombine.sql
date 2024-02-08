# use example
# params: batch_id
# CALL showShortestCombines (990040);

DELIMITER //
DROP PROCEDURE IF EXISTS showShortestCombines;
CREATE PROCEDURE showShortestCombines(
	in batchId INT
)

LABEL_PROC:
BEGIN
	SELECT @row_num:=@row_num+1 AS '#',combine_number,combine_cost as total_cost,path,path_cost,shared_path,shared_path_cost FROM combination c,(SELECT @row_num := 0) r WHERE batch_id=batchId;
END //
DELIMITER ;