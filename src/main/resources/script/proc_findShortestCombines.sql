# http://localhost/shortestPath/path/findShortestCombine/SOURCE_6/DEST1,DEST4/3
# use example
# params: source_name, target_name, combine_count, use_log, print_path
CALL findShortestCombines ('SOURCE_6','DEST1,DEST4',6, FALSE, TRUE);

DELIMITER //
DROP PROCEDURE IF EXISTS findShortestCombines;
CREATE PROCEDURE findShortestCombines(
	in source_name VARCHAR(50),
	in target_names VARCHAR(255),
	in combine_max INT,
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
	DECLARE e_cost INT;
	DECLARE c_id INT;
	DECLARE c_cost INT;
	DECLARE c_name VARCHAR(50);
	DECLARE c_type VARCHAR(20);
	#process
	DECLARE n_ids VARCHAR(255);
	DECLARE target_count INT default 0;
	DECLARE c_count INT default 0;
	DECLARE path_count INT default 0;
	DECLARE total_path_count INT default 0;
	DECLARE combine_count INT default 1;
	DECLARE name_cut INT default 10;
	DECLARE queue_empty INT default 0;
	DECLARE batchId INT default FLOOR(RAND()*1000000);

	#log and run time
	DECLARE log_type VARCHAR(255);
	DECLARE start_time_ms DECIMAL(15, 3);
	DECLARE end_time_ms DECIMAL(15, 3);
    #cursors
	DECLARE childNodeCur CURSOR FOR
	SELECT e.destination_device,e.weight,n.cost,n.name,n.type FROM connections e JOIN devices n ON e.destination_device = n.id WHERE e.source_device = n_id AND n.status='Operational' ORDER BY e.weight;

	IF enable_log!=1 THEN
		SET enable_log = 0;
	END IF;
	IF print_path!=1 THEN
		SET print_path = 0;
	END IF;
	# def log type
	SET log_type = concat('findShortestCombines(',source_name,',',target_names,',',combine_max,',',enable_log,')');
	SET start_time_ms=UNIX_TIMESTAMP(NOW(6));
	# valid check
	IF combine_max<1 THEN
		INSERT INTO log (type,message) VALUE (log_type,'combine_max shouldn\'t less than 1.');
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
	INSERT INTO temp_path (current_node_id,type,comulative_cost,depth,node_ids,node_costs,edge_costs,node_names) SELECT
	id,type,cost,0,concat(',',id,','),cost,0,concat(LEFT(name, name_cut)) FROM devices WHERE id = source_id AND type='Source';
	IF enable_log THEN
		INSERT INTO log (type,message) VALUE (log_type,concat('enqueue:',source_id));
	END IF;
	# loop targets
	ltarget: LOOP
		SET target_count = target_count+1;
		SELECT trim(substring_index(substring_index(target_names,',',target_count),',',-1)) INTO temp_name;
		# reach the end
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
		SET target_ids=concat(target_ids,',',target_id);
		SELECT COUNT(1) INTO path_count FROM temp_path WHERE current_node_id=target_id AND finish=1 ORDER BY comulative_cost ASC, depth ASC LIMIT combine_max;
		IF combine_count*path_count<combine_max AND NOT queue_empty THEN
			que:LOOP
				# dequeue
				SELECT id,current_node_id,comulative_cost,type,depth,node_ids
				INTO r_id,n_id,n_comulative_cost,n_type,n_depth,n_ids
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
						INSERT INTO log (type,message) VALUE (log_type,concat('visit target (path_count:',path_count,',combine_count:',combine_count,')'));
					END IF;
					UPDATE temp_path SET finish=1 WHERE id=r_id;
					IF path_count>0 AND combine_count*path_count>=combine_max THEN
						LEAVE que;
					ELSE
						ITERATE que;
					END IF;
				END IF;
				# previous target, skip
				IF locate(concat(',',n_id,','),concat(target_ids,',')) THEN
					UPDATE temp_path SET finish=2 WHERE current_node_id=n_id AND finish=0;
					ITERATE que;
				END IF;
				# achieve the end of a path
				SELECT COUNT(*) INTO c_count FROM connections e JOIN devices n ON e.destination_device = n.id WHERE e.source_device = n_id AND n.status='Operational';
				IF c_count=0 THEN
					UPDATE temp_path SET finish=1 WHERE current_node_id=n_id;
				# normal case
				ELSE
					SET @updatedPath=FALSE;
					# loop children nodes
					OPEN childNodeCur;
					cnr:LOOP
						# children nodes have been expanded
						IF c_count=0 THEN
							# cycle happened at the last loop
							IF NOT @updatedPath THEN
								UPDATE temp_path SET finish=1 WHERE id=r_id;
							END IF;
							LEAVE cnr;
						END IF;
						FETCH childNodeCur INTO c_id, e_cost, c_cost, c_name, c_type;
						# check cycle
						IF locate(concat(',',c_id,','),n_ids) THEN
							SET c_count=c_count-1;
							IF enable_log THEN
								INSERT INTO log (type,message) VALUE (log_type,concat('cycle point, c_id:',c_id,', c_name:',c_name,' ',@available_count));
							END IF;
							ITERATE cnr;
						END IF;
						IF enable_log THEN
							INSERT INTO log (type,message) VALUE (log_type,concat('enqueue, c_id:',c_id,', c_name:',c_name,', e_cost:',e_cost,', c_cost:',c_cost,',comulative_cost:',n_comulative_cost+e_cost+c_cost,', c_type:',c_type));
						END IF;
						# update for the last child
						IF c_count=1 THEN
							SET @updatedPath=TRUE;
							UPDATE temp_path SET current_node_id=c_id,type=c_type,comulative_cost=comulative_cost+c_cost+e_cost,depth=depth+1,
							node_ids=concat(node_ids,c_id,','),node_costs=concat(node_costs,',',c_cost),edge_costs=concat(edge_costs,',',e_cost),node_names=concat(node_names,'->',LEFT(c_name, name_cut))
							WHERE id=r_id;
						# insert for the first n-1 children
						ELSE
							INSERT INTO temp_path (current_node_id,type,comulative_cost,depth,node_ids,node_costs,edge_costs,node_names)
							SELECT c_id,c_type,comulative_cost+c_cost+e_cost,depth+1,concat(node_ids,c_id,','),concat(node_costs,',',c_cost),concat(edge_costs,',',e_cost),concat(node_names,'->',LEFT(c_name, name_cut))
							FROM temp_path WHERE id=r_id;
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
		# update count
		SET total_path_count=total_path_count+path_count;
		IF path_count>0 THEN
			SET combine_count=combine_count*path_count;
		END IF;
	END LOOP ltarget;

	SET end_time_ms=UNIX_TIMESTAMP(NOW(6));
	INSERT INTO log (type,message) VALUE (log_type,concat('total time usage:',TRUNCATE((end_time_ms - start_time_ms)*1000,0),'ms'));
	SELECT batchId;
	# show paths
	IF print_path THEN
		SET @strSql = CONCAT('SELECT current_node_id,comulative_cost,node_ids,node_costs,edge_costs,node_names FROM temp_path WHERE current_node_id IN (',SUBSTRING(target_ids,2),') AND finish=1 ORDER BY comulative_cost ASC, depth ASC LIMIT ',total_path_count);
		PREPARE preparedStatement FROM @strSql;
		EXECUTE preparedStatement;
		DEALLOCATE PREPARE preparedStatement;
	END IF;
END //
DELIMITER ;