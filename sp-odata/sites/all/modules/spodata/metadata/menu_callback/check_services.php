<?php
/**
 * Check if all soap and http services are running.
 */
function metadata_check_services() {

	$results = array();
	$services = MetadataHelper::get_services();
	$db_name = variable_get('metadata_odata_db_name', 'OpenDataDomain');
	
	// featch all services
	foreach ($services as $service_info) {
		
		$service_status = _check_service($service_info, $db_name);
		
		// set output
		$results[$service_info->service_name] = $service_status;
		
	}
	
	// output
	return theme('metadata_check_services', array('services' => $results, 'storage' => $db_name));
}



/**
 * Check if service is running.
 * 
 * @param object $service_info (id,service_name,service_url)
 * @param string $db_name storage name.
 */
function _check_service($service_info, $db_name) {
	if (strtoupper($service_info->type)=='SOAP')  {
		try {
			$handle = curl_init($service_info->service_url);
			curl_setopt($handle,  CURLOPT_RETURNTRANSFER, TRUE);
			
			// test URL
			$response = curl_exec($handle);
			$httpCode = curl_getinfo($handle, CURLINFO_HTTP_CODE);
			if($httpCode == 404 || !$response) {
				/* You don't have a WSDL Service is down. exit the function */
				return array( 'status' => false, 'status_text' => "You don't have a WSDL Service is down. exit the function");
			}
			curl_close($handle);
			
			// call soap service
			$client = @new SoapClient($service_info->service_url . '?wsdl', array( 'exceptions' => true, 'trace' => 1));
			$result = $client->checkConfiguration(array('name' => $db_name));
			
			// read response result
			if(isset($result->checkConfigurationReturn)) {
				if($result->checkConfigurationReturn>=0)
					return array( 'status' => true, 'status_text' => t('Servizio attivo'));
				else 
					return array( 'status' => false, 'status_text' => t('Errore nella configurazione'));
			}
			else 
				return array( 'status' => false, 'status_text' => t('Il servizio non risponde'));
		}
		catch (Exception $ex) {
			return array( 'status' => false, 'status_text' => $ex->getMessage());
		}
		catch (SoapFault $ex) {
			return array( 'status' => false, 'status_text' => $ex->getMessage());
		}
	}
	else if (strtoupper($service_info->type)=='HTTP'){
		// MdData?checkConfiguration=dbName&$format=json   http://10.0.0.167:9091/InMemoryProducerExample.svc/MdData?checkConfiguration=sp_opendata_gisdemo_test&$format=json
		$response = drupal_http_request($service_info->service_url . 'MdData?checkConfiguration=' . $db_name . '&$format=json');
		//$data_obj->d->results[0]->Res
		if(isset($response->error))
			return array( 'status' => false,  'status_text' => $response->error);
		else {
			if(isset($response->data)) {
				$data_obj = json_decode($response->data);
				if(isset($data_obj->d->results[0]->Res)) {
					if($data_obj->d->results[0]->Res>=0)
						return array( 'status' => true,  'status_text' => t('Servizio attivo'));
					else 
						return array( 'status' => false, 'status_text' => t('Errore nella configurazione'));
				}
				else {
					return array( 'status' => false,  'status_text' => t('Il servizio non risponde'));
				}
			}
			else
				return array( 'status' => false,  'status_text' => t('Il servizio non risponde'));
		}
	}
	
	// not handled services
	return array( 'status' => true, 'status_text' => 'check da gestire');
}