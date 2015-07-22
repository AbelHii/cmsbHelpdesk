<?php
    $link = mysqli_connect('127.0.0.1','abel', 'abel', 'chd');  
    
    if(mysqli_connect_errno()){
        echo "nooo" . mysqli_connect_error();
    }
    else{
    	$query = "SELECT id 
                	FROM hd_case 
                	ORDER BY id DESC 
                	LIMIT 1";         

        $result = mysqli_query($link, $query) or die(mysqli_error($link));
        // array for JSON response
        $response = array();

        if ($result) {
            $row = mysqli_fetch_assoc($result);
            $response["success"] = 1;
        	$response["max_id"] = $row["id"];
        }
        echo json_encode($response);
    }   
?>