<?php
    
    require '../../config/database.php';
    require '../../libs/Database.php';
    $db = new Database( DB_TYPE, DB_HOST, DB_NAME , DB_USER , DB_PASS);

    $username = $_GET["username"];
    $query = "SELECT hd_case.id AS caseid, 
                    hd_case.descr,
                    hd_case.assignee_id AS casea, 
                    hd_case.status_id AS status_id, 
                    hd_case.user_id AS caseuser,
                    hd_case.actiontaken,
                    hd_user.id,
                    hd_user.fullname AS name,
                    hd_status.id,
                    hd_status.status,
                    hd_admin.id AS login_id,
                    hd_admin.login AS aname
                FROM `hd_case`
                LEFT JOIN `hd_user` ON  hd_case.user_id = hd_user.id 
                LEFT JOIN `hd_admin` ON hd_case.assignee_id = hd_admin.id
                LEFT JOIN `hd_status` ON hd_case.status_id = hd_status.id
                WHERE hd_admin.login = '$username'
                ORDER BY hd_case.id DESC
                LIMIT 0, 30";                    


    //$result = mysqli_query($link, $query) or die(mysqli_error($link));
    $sth = $db->prepare($query);
    $result = $sth->execute();
    // array for JSON response
    $response = array();

    if ($result) {
        // looping through all results
        // emp node
        $response["caseslist"] = array();

        while ($row = $sth->fetchAll()) {
            $response["success"] = 1;
            // temp user array
                $caseslist = array();
                $caseslist["id"] = $row["caseid"];
                $caseslist["description"] = $row["descr"];
                $caseslist["assignee"] = $row["aname"];
                $caseslist["status"] = $row["status"];
                $caseslist["user"] = $row["name"];
                $caseslist["actiontaken"] = $row["actiontaken"];
                $caseslist["login_id"] = $row["login_id"];
                $caseslist["status_id"] = $row["status_id"];
            // push single Case into final response array
            array_push($response["caseslist"], $caseslist);
        }
        // success
        $response["message"] = "Cases Loaded";

        // echoing JSON response
        echo json_encode($response);
    } else {
        // no emp found
        $response["success"] = 0;
        $response["message"] = "No Cases found";

        // echo no cases JSON
        echo json_encode($response);
    }

?>