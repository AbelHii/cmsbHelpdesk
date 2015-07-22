<?php
    
    require '../../config/database.php';
    require '../../libs/Database.php';
    $db = new Database( DB_TYPE, DB_HOST, DB_NAME , DB_USER , DB_PASS);

    $password=$_POST["password"];
    $username=$_POST["username"];

    if (!empty($_POST)) {
          if (empty($_POST['username']) || empty($_POST['password'])) {
            // Create some data that will be the JSON response 
            $response["success"] = 0;
            $response["message"] = " username or password fields are empty ";

            //die is used to kill the page, will not let the code below to be executed. It will also
            //display the parameter, that is the json data which our android application will parse to be //shown to the users
            die(json_encode($response));
          }
          $query = " SELECT hd_admin.id,
                            hd_admin.login,
                            hd_admin.password
                      FROM  `hd_admin` 
                      WHERE login = '$username'
                      AND password =  MD5('$password')";


          //$sql1=mysqli_query($link, $query) or die(json_encode(mysqli_error($link)));
          $sth = $db->prepare($query);
          $result = $sth->execute();
          $result = $sth->fetch(PDO::FETCH_ASSOC);
          //$row = mysqli_fetch_assoc($sql1);
        
          if (!empty($result)) {
              $response["loginID"] = (isset($result['id']) ? $result['id'] : null);
              $response["success"] = 1;
              $response["message"] = "You have been sucessfully logged in";
              die(json_encode($response));
          }
          else{
              $response["success"] = 0;
              $response["message"] = "invalid username or password ";
              die(json_encode($response));
          }
                
    }
    else{
      $response["success"] = 0;
      $response["message"] = " username or password fields are empty ";
      die(json_encode($response));
    }          

  ?>
