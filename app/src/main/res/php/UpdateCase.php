<?php
 
    require '../../config/database.php';
    require '../../libs/Database.php';
    $db = new Database( DB_TYPE, DB_HOST, DB_NAME , DB_USER , DB_PASS);

    $id = $_POST["id"];
    $user_id = $_POST["user_id"];
    $description = $_POST["description"];
    $actiontaken = $_POST["actiontaken"];
    $statusID = $_POST["status"];
    
    $response = array();

    if(!empty($_POST)){
        $query = "UPDATE `chd`.`hd_case` 
                    SET user_id = '$user_id',
                    `descr` = '$description', 
                     actiontaken = '$actiontaken',
                     status_id = '$statusID'
                    WHERE `hd_case`.`id` = '$id' 
                    LIMIT 1";


        //$result = mysqli_query($link, $query) or die(mysqli_error($link));

        $sth = $db->prepare($query);
        $result = $sth->execute();

        if($result){
            // success
            $response["success"] = 1;
            $response["message"] = "Updated Case ";   
        }else{
            $response["success"] = 0;
            $response["message"] = "Failed to Update";
        }

        // return json
        print json_encode($response);

    }
    else{
        $response["success"] = 0;
        $response["message"] = "Failed to Update";

        //return json
        print json_encode($response);
    }

?>