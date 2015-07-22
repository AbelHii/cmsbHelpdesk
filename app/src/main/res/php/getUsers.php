<?php
    $link = mysqli_connect('127.0.0.1','abel', 'abel', 'chd');  
    
    if(mysqli_connect_errno()){
        echo "nooo" . mysqli_connect_error();
    }
    else{
        //get the users for the users spinner--------------------------
        $query2 = "SELECT hd_user . * , 
                        hd_division.name AS company_name
                    FROM  `hd_user` 
                    LEFT JOIN hd_division ON hd_user.division_id = hd_division.id
                    WHERE hd_division.enabled = '1'
                    ORDER BY hd_user.fullname";

        $result2 = mysqli_query($link, $query2) or die(mysqli_error($link));

        // array for JSON response
        $usersResponse = array();    
                
        if (mysqli_num_rows($result2) > 0) {
            // looping through all results
            // emp node
            $usersResponse["userslist"] = array();

            while ($row = mysqli_fetch_assoc($result2)) {
                $usersResponse["success"] = 1;
                // temp user array
                    $userslist = array();
                    $userslist["userId"] = $row["id"];
                    $userslist["name"] = $row["fullname"];
                    $userslist["telephone"] = $row["telephone"];
                    $userslist["email"] = $row["email"];
                    $userslist["company"] = $row["company_name"];
                // push single User into final response array
                array_push($usersResponse["userslist"], $userslist);
            }
        
        // success
        $usersResponse["message"] = "Users Loaded";

        // echoing JSON response
        echo json_encode($usersResponse);
        } else {
            // no emp found
            $usersResponse["success"] = 0;
            $usersResponse["message"] = "No Cases found";

            // echo no users JSON
            echo json_encode($usersResponse);
        }

    }

    mysqli_close($link);
?>