package com.iemr.ecd.controller.quality;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iemr.ecd.dao.AgentQualityAuditorMap;
import com.iemr.ecd.service.quality.AgentQualityAuditorMappingImpl;
import com.iemr.ecd.utils.advice.exception_handler.CustomExceptionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping(value = "/agentQualityAuditorMap", headers = "Authorization")
@CrossOrigin()
public class AgentQualityAuditorMappingController {
	@Autowired
	private AgentQualityAuditorMappingImpl agentQualityAuditorMappingImpl;
	
	@PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Create Agent quality auditor mapping", description = "Desc - Create Agent quality auditor mapping")
	@ApiResponses(value = {
			@ApiResponse(responseCode = CustomExceptionResponse.SUCCESS_SC_V, description = CustomExceptionResponse.SUCCESS_SC, content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = CustomExceptionResponse.NOT_FOUND_SC_V, description = CustomExceptionResponse.NOT_FOUND_SC),
			@ApiResponse(responseCode = CustomExceptionResponse.INTERNAL_SERVER_ERROR_SC_V, description = CustomExceptionResponse.INTERNAL_SERVER_ERROR_SC),
			@ApiResponse(responseCode = CustomExceptionResponse.DB_EXCEPTION_SC_V, description = CustomExceptionResponse.DB_EXCEPTION_SC),
			@ApiResponse(responseCode = CustomExceptionResponse.BAD_REQUEST_SC_V, description = CustomExceptionResponse.BAD_REQUEST_SC)})
	public ResponseEntity<String> createAgentQualityAuditorMapping(
			@RequestBody AgentQualityAuditorMap agentQualityAuditorMap) {
		
		return new ResponseEntity<>(agentQualityAuditorMappingImpl.createAgentQualityAuditorMapping(agentQualityAuditorMap), HttpStatus.OK);
	}
	
	@GetMapping(value = "/getByPSMId/{psmId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Fetch Agent quality auditor mapping", description = "Desc - Fetch Agent quality auditor mapping")
	@ApiResponses(value = {
			@ApiResponse(responseCode = CustomExceptionResponse.SUCCESS_SC_V, description = CustomExceptionResponse.SUCCESS_SC, content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = CustomExceptionResponse.NOT_FOUND_SC_V, description = CustomExceptionResponse.NOT_FOUND_SC),
			@ApiResponse(responseCode = CustomExceptionResponse.INTERNAL_SERVER_ERROR_SC_V, description = CustomExceptionResponse.INTERNAL_SERVER_ERROR_SC),
			@ApiResponse(responseCode = CustomExceptionResponse.DB_EXCEPTION_SC_V, description = CustomExceptionResponse.DB_EXCEPTION_SC),
			@ApiResponse(responseCode = CustomExceptionResponse.BAD_REQUEST_SC_V, description = CustomExceptionResponse.BAD_REQUEST_SC)})
	public ResponseEntity<List<AgentQualityAuditorMap>> getAgentQualityAuditorMappingByPSMId(@PathVariable Integer psmId) {
		return new ResponseEntity<>(agentQualityAuditorMappingImpl.getAgentQualityAuditorMappingByPSMId(psmId), HttpStatus.OK);
	}
	
	@PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Update Agent quality auditor mapping", description = "Desc - Update Agent quality auditor mapping")
	@ApiResponses(value = {
			@ApiResponse(responseCode = CustomExceptionResponse.SUCCESS_SC_V, description = CustomExceptionResponse.SUCCESS_SC, content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = CustomExceptionResponse.NOT_FOUND_SC_V, description = CustomExceptionResponse.NOT_FOUND_SC),
			@ApiResponse(responseCode = CustomExceptionResponse.INTERNAL_SERVER_ERROR_SC_V, description = CustomExceptionResponse.INTERNAL_SERVER_ERROR_SC),
			@ApiResponse(responseCode = CustomExceptionResponse.DB_EXCEPTION_SC_V, description = CustomExceptionResponse.DB_EXCEPTION_SC),
			@ApiResponse(responseCode = CustomExceptionResponse.BAD_REQUEST_SC_V, description = CustomExceptionResponse.BAD_REQUEST_SC)})
	public ResponseEntity<String> updateAgentQualityAuditorMapping(@RequestBody AgentQualityAuditorMap agentQualityAuditorMap) {
		return new ResponseEntity<>(agentQualityAuditorMappingImpl.updateAgentQualityAuditorMapping(agentQualityAuditorMap), HttpStatus.OK);
	}

}