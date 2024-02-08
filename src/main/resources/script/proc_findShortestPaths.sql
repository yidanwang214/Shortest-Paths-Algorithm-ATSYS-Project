
# use example
# params: source_name, target_name, path_count, use_log, print_path
# CALL findShortestPaths ('Device A','Device E,Device G',3, FALSE, TRUE);

DELIMITER //
DROP PROCEDURE IF EXISTS findShortestPaths;
CREATE PROCEDURE findShortestPaths(
	in source_name VARCHAR(50),
	in target_names VARCHAR(255),
	in path_max INT,
	in enable_log INT,
	in print_path INT
)

LABEL_PROC:
BEGIN
	DECLARE source_id INT;
	DECLARE target_id INT;
	DECLARE target_name VARCHAR(50) default '';
	DECLARE temp_name VARCHAR(50);
	DECLARE target_ids VARCHAR(255) default '';
	#current
	DECLARE r_id INT;
	DECLARE n_id INT;
	DECLARE n_comulative_cost INT;
	DECLARE n_type VARCHAR(20);
	DECLARE n_depth INT;
	#child
	DECLARE c_id INT;
	DECLARE c_cost INT;
	DECLARE c_name VARCHAR(50);
	DECLARE c_type VARCHAR(20);
	#process
	DECLARE target_count INT default 0;
	DECLARE c_count INT default 0;
	DECLARE path_count INT default 0;
	DECLARE tmp_path_count INT;
	DECLARE name_cut INT default 10;
	DECLARE queue_empty INT default 0;
	DECLARE batchId INT default FLOOR(RAND()*1000000);
	#log and run time
	DECLARE log_type VARCHAR(255);
	DECLARE start_time_ms DECIMAL(15, 3);
	DECLARE end_time_ms DECIMAL(15, 3);
    #cursors
	DECLARE childNodeCur CURSOR FOR
	SELECT e.destination_device,e.weight+n.cost,n.name,n.type FROM connections e JOIN devices n ON e.destination_device = n.id WHERE e.source_device = n_id AND n.status='Operational' ORDER BY e.weight;

	IF enable_log!=1 THEN
		SET enable_log = 0;
	END IF;
	IF print_path!=1 THEN
		SET print_path = 0;
	END IF;
	# def log type
	SET log_type = concat('findShortestPaths(',source_name,',',target_names,',',path_max,',',enable_log,')');
	SET start_time_ms=UNIX_TIMESTAMP(NOW(6));
	# valid check
	IF path_max<1 THEN
		INSERT INTO log (type,message) VALUE (log_type,'path_max shouldn\'t less than 1.');
	LEAVE LABEL_PROC;
	END IF;
	SELECT id INTO source_id FROM devices WHERE name=source_name AND type='Source' AND status='Operational';
	IF FOUND_ROWS()=0 THEN
		INSERT INTO log (type,message) VALUE (log_type,'source doesn\'t exist or unavailable.');
		LEAVE LABEL_PROC;
	END IF;
	SELECT 1 FROM connections WHERE source_device=source_id;
	IF FOUND_ROWS()=0 THEN
		INSERT INTO log (type,message) VALUE (log_type,'no path from the source.');
		LEAVE LABEL_PROC;
	END IF;

	# init queue for algorithm
	DELETE FROM temp_path;
	INSERT INTO temp_path (current_node_id,type,comulative_cost,depth,node_ids,node_names) SELECT
	id,type,cost,0,id,concat(LEFT(name, name_cut)) FROM devices WHERE id = source_id AND type='Source';
	IF enable_log THEN
		INSERT INTO log (type,message) VALUE (log_type,concat('enqueue:',source_id));
	END IF;
	# loop targets
	ltarget: LOOP
		SET target_count = target_count+1;
		SELECT trim(substring_index(substring_index(target_names,',',target_count),',',-1)) INTO temp_name;
		IF target_name=temp_name THEN
			LEAVE ltarget;
		END IF;
		SET target_name = temp_name;
		IF enable_log THEN
			INSERT INTO log (type,message) VALUE (log_type,concat('find path for target:',target_name));
		END IF;
		# valid check
		SELECT id INTO target_id FROM devices WHERE name=target_name AND type='Destination' AND status='Operational';
		IF FOUND_ROWS()=0 THEN
			INSERT INTO log (type,message) VALUE (log_type,'target (',target_name,') doesn\'t exist or unavailable.');
			ITERATE ltarget;
		END IF;
		SELECT 1 FROM connections WHERE destination_device=target_id;
		IF FOUND_ROWS()=0 THEN
			INSERT INTO log (type,message) VALUE (log_type,'no path to the target (',target_name,').');
			ITERATE ltarget;
		END IF;

		# init
		SET target_ids=concat(target_ids,target_id, ',');
		SELECT COUNT(*) INTO tmp_path_count FROM temp_path WHERE current_node_id=target_id AND finish=1 ORDER BY comulative_cost ASC, depth ASC LIMIT path_max;
		SET path_count=path_count+tmp_path_count;
		IF path_count<path_max AND NOT queue_empty THEN
			que:LOOP
				# dequeue
				SELECT id,current_node_id,comulative_cost,type,depth
				INTO r_id,n_id,n_comulative_cost,n_type,n_depth
				FROM temp_path WHERE finish=0 ORDER BY comulative_cost ASC,depth ASC LIMIT 1;
				# finished the entire graph seaching
				IF FOUND_ROWS()=0 THEN
					SET queue_empty = 1;
					LEAVE que;
				END IF;
				IF enable_log THEN
					INSERT INTO log (type,message) VALUE (log_type,concat('dequeue:',n_id));
				END IF;
				# achieve the target
				IF n_id=target_id THEN
					SET path_count=path_count+1;
					IF enable_log THEN
						INSERT INTO log (type,message) VALUE (log_type,concat('visit target.(',path_count,')'));
					END IF;
					UPDATE temp_path SET finish=1 WHERE id=r_id;
					IF path_count=path_max THEN
						LEAVE que;
					ELSE
						ITERATE que;
					END IF;
				END IF;

				SELECT COUNT(*) INTO c_count FROM connections e JOIN devices n ON e.destination_device = n.id WHERE e.source_device = n_id AND n.status='Operational';
				# achieve the end of a path
				IF c_count=0 THEN
					UPDATE temp_path SET finish=1 WHERE current_node_id=n_id;
				# normal case
				ELSE
					# loop children nodes
					OPEN childNodeCur;
					cnr:LOOP
							# children nodes have been expanded
						IF c_count=0 THEN
							LEAVE cnr;
						END IF;
						FETCH childNodeCur INTO c_id, c_cost, c_name, c_type;
						IF enable_log THEN
							INSERT INTO log (type,message) VALUE (log_type,concat('enqueue, c_id:',c_id,', c_name:',c_name,', c_cost:',c_cost,',comulative_cost:',n_comulative_cost+c_cost,', c_type:',c_type));
						END IF;
						# update for the last child
						IF c_count=1 THEN
							UPDATE temp_path SET current_node_id=c_id,type=c_type,comulative_cost=comulative_cost+c_cost,depth=depth+1,
							node_ids=concat(node_ids,',',c_id),node_names=concat(node_names,'->',LEFT(c_name, name_cut))
							WHERE current_node_id=n_id;
						# insert for the first n-1 children
						ELSE
							INSERT INTO temp_path (current_node_id,type,comulative_cost,depth,node_ids,node_names)
							SELECT c_id,c_type,comulative_cost+c_cost,depth+1,concat(node_ids,',',c_id),concat(node_names,'->',LEFT(c_name, name_cut))
							FROM temp_path WHERE current_node_id=n_id;
						END IF;
						SET c_count=c_count-1;
					END LOOP cnr;
					CLOSE childNodeCur;
				END IF;
			END LOOP que;
		END IF;
		IF enable_log THEN
			INSERT INTO log (type,message) VALUE (log_type,concat('find ',path_count,' path for ',target_name));
		END IF;
	END LOOP ltarget;
	# save path
	SELECT LEFT(target_ids,(SELECT LENGTH(target_ids))-1) INTO target_ids;
	SET @strSql = CONCAT('INSERT INTO path (batch_id,source,target,total_cost,total_node,path,created_by) SELECT
		',batchId,',',source_id,',',target_id,',comulative_cost,depth+1,node_names,"findPath4MultiTarget" FROM temp_path WHERE current_node_id IN (',target_ids,') ORDER BY comulative_cost ASC, depth ASC LIMIT ',path_max);
	PREPARE preparedStatement FROM @strSql;
	EXECUTE preparedStatement;
	DEALLOCATE PREPARE preparedStatement;

	SET end_time_ms=UNIX_TIMESTAMP(NOW(6));
	INSERT INTO log (type,message) VALUE (log_type,concat('total time usage:',TRUNCATE((end_time_ms - start_time_ms)*1000,0),'ms'));
	IF print_path THEN
		SELECT @row_num:=@row_num+1 AS '#', p.path as 'path', p.total_cost as 'path cost' FROM path p,(SELECT @row_num := 0) r WHERE p.batch_id = batchId ORDER BY p.total_cost LIMIT path_max;
	END IF;
END //
DELIMITER ;