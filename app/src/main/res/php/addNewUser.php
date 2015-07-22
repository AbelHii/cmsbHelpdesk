
<?php
    require '../../config/database.php';
    require '../../libs/Database.php';
    $db = new Database( DB_TYPE, DB_HOST, DB_NAME , DB_USER , DB_PASS);

    $name = $_GET["name"];
    $telephone = $_GET["telephone"];
    $email = $_GET["email"];
    $company = $_GET["company"];
    
    if(!empty($_GET)){
        $query = "INSERT INTO `chd`.`hd_user` 
                    (`id`, 
                    `login`, 
                    `password`, 
                    `fname`, 
                    `fullname`, 
                    `email`, 
                    `telephone`, 
                    `division_id`, 
                    `enabled`) 
                        VALUES 
                        (NULL, 
                            '', 
                            '', 
                            '', 
                            '$name', 
                            '$email', 
                            '$telephone', 
                            '$company', 
                            '1')";

        //$result = mysqli_query($link, $query) or die(mysqli_error($link));
        $sth = $db->prepare($query);
        $result = $sth->execute();

        
        if($result){
            // success
            $response["success"] = 1;
            $response["message"] = "Inserted New User";   
        }else{
            $response["success"] = 0;
            $response["message"] = "Name is empty";
        }

        // return json
        print json_encode($response);

    }
    else{
        $response["success"] = 0;
        $response["message"] = "Name is empty";

        //return json
        json_encode($response);
    }

?>