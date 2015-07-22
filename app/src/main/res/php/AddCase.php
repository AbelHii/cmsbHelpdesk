<?php
 
    require '../../config/database.php';
    require '../../libs/Database.php';
    $db = new Database( DB_TYPE, DB_HOST, DB_NAME , DB_USER , DB_PASS);
   
     $name = $_POST["name"];
     $description = $_POST["description"];
     $actiontaken = $_POST["actiontaken"];
     $assignee = $_POST["assignee"];
     $status = $_POST["status"];
        
     if(!empty($_POST)){
            $query = "INSERT INTO `chd`.`hd_case` 
                (`id`, 
                `descr`, 
                `actiontaken`, 
                `assignee_id`, 
                `user_id`, 
                `status_id`, 
                `opendate`, 
                `tag_hardware`, `tag_network`, `tag_application`, `tag_os`, `tag_printer`, `tag_virus`, `tag_sap`) 
                VALUES (
                    NULL,
                    '$description', 
                    '$actiontaken',
                    '$assignee', 
                    '$name', 
                    '$status', 
                    CURRENT_TIMESTAMP, 
                    '0', '0', '0', '0', '0', '0', '0')";


            $sth = $db->prepare($query);
		    $result = $sth->execute();

            $response = array();
                //success
    	    if ($result) {
    	        $response["success"] = 1;
            	$response["message"] = "Inserted new Case";
    	    }
    	    else if(!$result)
    	    {    
                $response["success"] = 0;    
                $response["message"] = "Failed to add case";       
            }    

            //return json
            echo json_encode($response);

        }
        else{
            $response["success"] = 0;
            $response["message"] = "Failed to add new case";

            //return json
            echo json_encode($response);
        }
   
?>
