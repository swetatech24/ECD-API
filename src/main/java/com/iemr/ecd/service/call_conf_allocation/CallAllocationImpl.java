package com.iemr.ecd.service.call_conf_allocation;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.iemr.ecd.dao.associate.ChildRecord;
import com.iemr.ecd.dao.associate.MotherRecord;
import com.iemr.ecd.dao.associate.OutboundCalls;
import com.iemr.ecd.dto.RequestCallAllocationDTO;
import com.iemr.ecd.dto.supervisor.ResponseEligibleCallRecordsDTO;
import com.iemr.ecd.repo.call_conf_allocation.ChildRecordRepo;
import com.iemr.ecd.repo.call_conf_allocation.MotherRecordRepo;
import com.iemr.ecd.repo.call_conf_allocation.OutboundCallsRepo;
import com.iemr.ecd.utils.advice.exception_handler.ECDException;
import com.iemr.ecd.utils.advice.exception_handler.InvalidRequestException;

import jakarta.transaction.Transactional;

@Service
public class CallAllocationImpl {

	@Autowired
	private MotherRecordRepo motherRecordRepo;
	@Autowired
	private OutboundCallsRepo outboundCallsRepo;
	@Autowired
	private ChildRecordRepo childRecordRepo;

	@Transactional(rollbackOn = Exception.class)
	public String allocateCalls(RequestCallAllocationDTO callAllocationDto) {

		try {
			if (callAllocationDto != null && callAllocationDto.getRoleName().equalsIgnoreCase("associate")
					&& callAllocationDto.getRecordType().equalsIgnoreCase("mother")) {
				return allocateMotherRecordsAssociates(callAllocationDto);
			} else if (callAllocationDto != null && callAllocationDto.getRoleName().equalsIgnoreCase("associate")
					&& callAllocationDto.getRecordType().equalsIgnoreCase("child")) {
				return allocateChildRecordsAssociates(callAllocationDto);
			} else if (callAllocationDto != null && callAllocationDto.getRoleName().equalsIgnoreCase("ANM")
					&& callAllocationDto.getRecordType().equalsIgnoreCase("mother")) {
				return allocateMotherRecordsToANM(callAllocationDto);
			} else if (callAllocationDto != null && callAllocationDto.getRoleName().equalsIgnoreCase("ANM")
					&& callAllocationDto.getRecordType().equalsIgnoreCase("child")) {
				return allocateChildRecordsToANM(callAllocationDto);
			} else if (callAllocationDto != null && callAllocationDto.getRoleName().equalsIgnoreCase("MO")
					&& callAllocationDto.getRecordType().equalsIgnoreCase("mother")) {
				return allocateMotherRecordsToMO(callAllocationDto);
			} else if (callAllocationDto != null && callAllocationDto.getRoleName().equalsIgnoreCase("MO")
					&& callAllocationDto.getRecordType().equalsIgnoreCase("child")) {
				return allocateChildRecordsToMO(callAllocationDto);
			} else
				throw new ECDException("please pass valid role and record type");
		} catch (Exception e) {
			throw new ECDException(e);
		}

	}

	private String allocateMotherRecordsAssociates(RequestCallAllocationDTO callAllocationDto) throws ParseException {
		List<OutboundCalls> outBoundCallList = new ArrayList<>();
		int totalRecordToAllocate = 0;
		if (callAllocationDto != null && callAllocationDto.getToUserIds() != null
				&& callAllocationDto.getToUserIds().length > 0 && callAllocationDto.getNoOfCalls() > 0)
			totalRecordToAllocate = (callAllocationDto.getToUserIds().length) * (callAllocationDto.getNoOfCalls());
		else
			throw new InvalidRequestException();

		if (totalRecordToAllocate <= 0)
			throw new InvalidRequestException();
		else {

			Timestamp tempFDateStamp = null;
			Timestamp tempTDateStamp = null;
			if (callAllocationDto.getFDate() != null && callAllocationDto.getTDate() != null) {
				tempFDateStamp = getTimestampFromString(
						callAllocationDto.getFDate().split("T")[0].concat("T00:00:00+05:30"));
				tempTDateStamp = getTimestampFromString(
						callAllocationDto.getTDate().split("T")[0].concat("T23:59:59+05:30"));
			} else
				throw new InvalidRequestException("from date / to date is null");

			List<MotherRecord> resultSet = motherRecordRepo.getMotherRecordForAllocation(tempFDateStamp, tempTDateStamp,
					callAllocationDto.getPhoneNoType(), totalRecordToAllocate);

			OutboundCalls outboundCalls;

			int callCountPointer = 0;
			if (resultSet != null && resultSet.size() > 0) {
				List<Long> motherIds = new ArrayList<>();
				for (MotherRecord motherRecord : resultSet) {
					try {

						outboundCalls = new OutboundCalls();

						if (motherRecord.getEcdIdNo() != null)
							outboundCalls.setMotherId(motherRecord.getEcdIdNo());
						if (motherRecord.getBeneficiaryRegID() != null)
							outboundCalls.setBeneficiaryRegId(motherRecord.getBeneficiaryRegID());
						if (callAllocationDto.getPsmId() != null)
							outboundCalls.setPsmId(callAllocationDto.getPsmId());

						if (motherRecord.getWhomPhoneNo() != null)
							outboundCalls.setPhoneNumberType(motherRecord.getWhomPhoneNo());

						outboundCalls.setEcdCallType("introductory");
						outboundCalls.setDisplayEcdCallType("introductory");
						outboundCalls.setCallStatus("Open");
						outboundCalls.setAllocationStatus("allocated");
						outboundCalls.setAllocatedUserId(
								callAllocationDto.getToUserIds()[callCountPointer / callAllocationDto.getNoOfCalls()]);

						outboundCalls.setCreatedBy(callAllocationDto.getCreatedBy());
						if (motherRecord.getHighRisk() != null)
							outboundCalls.setIsHighRisk(motherRecord.getHighRisk());

						if (motherRecord.getHighRiskReason() != null)
							outboundCalls.setHighRiskReason(motherRecord.getHighRiskReason());

						if (motherRecord.getCreatedDate() != null)
							outboundCalls.setCallDateFrom(getTimestampDaysLater(motherRecord.getCreatedDate(), 0));

						outboundCalls.setCallDateTo(getTimestampDaysLater(motherRecord.getCreatedDate(), 30));

						outboundCalls.setCallAttemptNo(0);

						outBoundCallList.add(outboundCalls);

						callCountPointer++;

						motherIds.add(motherRecord.getEcdIdNo());
					} catch (Exception e) {
						// log
						callCountPointer++;
					}
				}
				outboundCallsRepo.saveAll(outBoundCallList);

				int i = motherRecordRepo.updateIsAllocatedStatus(motherIds);

				Map<String, Object> responseMap = new HashMap<>();
				responseMap.put("response", outBoundCallList.size() + " mother record allocated successfully");
				return new Gson().toJson(responseMap);
			} else
				throw new ECDException("no eligible record available to allocate, please contact administrator");

		}
	}

	private String allocateChildRecordsAssociates(RequestCallAllocationDTO callAllocationDto) throws ParseException {
		List<OutboundCalls> outBoundCallList = new ArrayList<>();
		int totalRecordToAllocate = 0;

		if (callAllocationDto != null && callAllocationDto.getToUserIds() != null
				&& callAllocationDto.getToUserIds().length > 0 && callAllocationDto.getNoOfCalls() > 0)
			totalRecordToAllocate = (callAllocationDto.getToUserIds().length) * (callAllocationDto.getNoOfCalls());
		else
			throw new InvalidRequestException();

		if (totalRecordToAllocate <= 0)
			throw new InvalidRequestException();
		else {

			Timestamp tempFDateStamp = null;
			Timestamp tempTDateStamp = null;
			if (callAllocationDto.getFDate() != null && callAllocationDto.getTDate() != null) {
				tempFDateStamp = getTimestampFromString(
						callAllocationDto.getFDate().split("T")[0].concat("T00:00:00+05:30"));
				tempTDateStamp = getTimestampFromString(
						callAllocationDto.getTDate().split("T")[0].concat("T23:59:59+05:30"));
			} else
				throw new InvalidRequestException("from date / to date is null");

			List<ChildRecord> resultSet = childRecordRepo.getChildRecordForAllocation(tempFDateStamp, tempTDateStamp,
					callAllocationDto.getPhoneNoType(), totalRecordToAllocate);

			OutboundCalls outboundCalls;

			int callCountPointer = 0;

			if (resultSet != null && resultSet.size() > 0) {
				List<Long> childIds = new ArrayList<>();
				for (ChildRecord childRecord : resultSet) {

					try {
						outboundCalls = new OutboundCalls();

						if (childRecord.getEcdIdNoChildId() != null)
							outboundCalls.setChildId(childRecord.getEcdIdNoChildId());
						if (childRecord.getMotherId() != null)
							outboundCalls.setMotherId(childRecord.getMotherId());
						if (childRecord.getBeneficiaryRegId() != null)
							outboundCalls.setBeneficiaryRegId(childRecord.getBeneficiaryRegId());
						if (callAllocationDto.getPsmId() != null)
							outboundCalls.setPsmId(callAllocationDto.getPsmId());

						if (childRecord.getWhomPhoneNo() != null)
							outboundCalls.setPhoneNumberType(childRecord.getWhomPhoneNo());

						outboundCalls.setEcdCallType("introductory");
						outboundCalls.setDisplayEcdCallType("introductory");
						outboundCalls.setCallStatus("Open");
						outboundCalls.setAllocationStatus("allocated");
						outboundCalls.setAllocatedUserId(
								callAllocationDto.getToUserIds()[callCountPointer / callAllocationDto.getNoOfCalls()]);

						outboundCalls.setCreatedBy(callAllocationDto.getCreatedBy());

						outboundCalls.setIsHrni(childRecord.getIsHrni());
						outboundCalls.setHrniReason(childRecord.getHrni_Reason());

						if (childRecord.getCreatedDate() != null)
							outboundCalls.setCallDateFrom(getTimestampDaysLater(childRecord.getCreatedDate(), 0));

						outboundCalls.setCallDateTo(getTimestampDaysLater(childRecord.getCreatedDate(), 30));

						outboundCalls.setCallAttemptNo(0);

						outBoundCallList.add(outboundCalls);

						childIds.add(childRecord.getEcdIdNoChildId());
						callCountPointer++;
					} catch (Exception e) {
						callCountPointer++;
						// log
					}

				}
				outboundCallsRepo.saveAll(outBoundCallList);
				int i = childRecordRepo.updateIsAllocatedStatus(childIds);

				Map<String, Object> responseMap = new HashMap<>();
				responseMap.put("response", outBoundCallList.size() + " child record allocated successfully");
				return new Gson().toJson(responseMap);

			}
			throw new ECDException("no eligible record available to allocate, please contact administrator");

		}
	}

	private String allocateMotherRecordsToANM(RequestCallAllocationDTO callAllocationDto) throws ParseException {

		int totalRecordToAllocate = 0;

		if (callAllocationDto != null && callAllocationDto.getToUserIds() != null
				&& callAllocationDto.getToUserIds().length > 0 && callAllocationDto.getNoOfCalls() > 0)
			totalRecordToAllocate = (callAllocationDto.getToUserIds().length) * (callAllocationDto.getNoOfCalls());
		else
			throw new InvalidRequestException();

		Timestamp tempFDateStamp = null;
		Timestamp tempTDateStamp = null;
		if (callAllocationDto.getFDate() != null && callAllocationDto.getTDate() != null) {
			tempFDateStamp = getTimestampFromString(
					callAllocationDto.getFDate().split("T")[0].concat("T00:00:00+05:30"));
			tempTDateStamp = getTimestampFromString(
					callAllocationDto.getTDate().split("T")[0].concat("T23:59:59+05:30"));
		} else
			throw new InvalidRequestException("from date / to date is null");

		Pageable pageable = PageRequest.of(0, totalRecordToAllocate);

		Page<OutboundCalls> outboundCallsPage = outboundCallsRepo.getMotherRecordsForANM(pageable, "unallocated",
				callAllocationDto.getPsmId(), tempFDateStamp, tempTDateStamp);

		List<OutboundCalls> outboundCallsList = outboundCallsPage.getContent();

		int callCountPointer = 0;
		if (outboundCallsList != null && outboundCallsList.size() > 0) {
			for (OutboundCalls outboundCalls : outboundCallsList) {
				try {
					outboundCalls.setAllocationStatus("allocated");
					outboundCalls.setAllocatedUserId(
							callAllocationDto.getToUserIds()[callCountPointer / callAllocationDto.getNoOfCalls()]);

					outboundCalls.setCallAttemptNo(0);

					callCountPointer++;
				} catch (Exception e) {
					// TODO: handle exception
					callCountPointer++;
				}
			}

			outboundCallsRepo.saveAll(outboundCallsList);

			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("response",
					outboundCallsList.size() + " mother record allocated successfully to selected ANM");
			return new Gson().toJson(responseMap);

		} else
			throw new ECDException("no eligible record available to allocate, please contact administrator");

	}

	private String allocateChildRecordsToANM(RequestCallAllocationDTO callAllocationDto) throws ParseException {
		int totalRecordToAllocate = 0;

		if (callAllocationDto != null && callAllocationDto.getToUserIds() != null
				&& callAllocationDto.getToUserIds().length > 0 && callAllocationDto.getNoOfCalls() > 0)
			totalRecordToAllocate = (callAllocationDto.getToUserIds().length) * (callAllocationDto.getNoOfCalls());
		else
			throw new InvalidRequestException();

		Timestamp tempFDateStamp = null;
		Timestamp tempTDateStamp = null;
		if (callAllocationDto.getFDate() != null && callAllocationDto.getTDate() != null) {
			tempFDateStamp = getTimestampFromString(
					callAllocationDto.getFDate().split("T")[0].concat("T00:00:00+05:30"));
			tempTDateStamp = getTimestampFromString(
					callAllocationDto.getTDate().split("T")[0].concat("T23:59:59+05:30"));
		} else
			throw new InvalidRequestException("from date / to date is null");

		Pageable pageable = PageRequest.of(0, totalRecordToAllocate);

		Page<OutboundCalls> outboundCallsPage = outboundCallsRepo.getChildRecordsForANM(pageable, "unallocated",
				callAllocationDto.getPsmId(), tempFDateStamp, tempTDateStamp);

		List<OutboundCalls> outboundCallsList = outboundCallsPage.getContent();

		int callCountPointer = 0;
		if (outboundCallsList != null && outboundCallsList.size() > 0) {
			for (OutboundCalls outboundCalls : outboundCallsList) {
				try {
					outboundCalls.setAllocationStatus("allocated");
					outboundCalls.setAllocatedUserId(
							callAllocationDto.getToUserIds()[callCountPointer / callAllocationDto.getNoOfCalls()]);

					outboundCalls.setCallAttemptNo(0);

					callCountPointer++;
				} catch (Exception e) {
					// log
					callCountPointer++;
				}
			}

			outboundCallsRepo.saveAll(outboundCallsList);

			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("response",
					outboundCallsList.size() + " child record allocated successfully to selected ANM");
			return new Gson().toJson(responseMap);
		} else
			throw new ECDException("no eligible record available to allocate, please contact administrator");

	}

	private String allocateMotherRecordsToMO(RequestCallAllocationDTO callAllocationDto) throws ParseException {
		int totalRecordToAllocate = 0;

		if (callAllocationDto != null && callAllocationDto.getToUserIds() != null
				&& callAllocationDto.getToUserIds().length > 0 && callAllocationDto.getNoOfCalls() > 0)
			totalRecordToAllocate = (callAllocationDto.getToUserIds().length) * (callAllocationDto.getNoOfCalls());
		else
			throw new InvalidRequestException();

		Timestamp tempFDateStamp = null;
		Timestamp tempTDateStamp = null;
		if (callAllocationDto.getFDate() != null && callAllocationDto.getTDate() != null) {
			tempFDateStamp = getTimestampFromString(
					callAllocationDto.getFDate().split("T")[0].concat("T00:00:00+05:30"));
			tempTDateStamp = getTimestampFromString(
					callAllocationDto.getTDate().split("T")[0].concat("T23:59:59+05:30"));
		} else
			throw new InvalidRequestException("from date / to date is null");

		Pageable pageable = PageRequest.of(0, totalRecordToAllocate);

		Page<OutboundCalls> outboundCallsPage = outboundCallsRepo.getMotherRecordsForMO(pageable, "unallocated",
				callAllocationDto.getPsmId(), tempFDateStamp, tempTDateStamp);

		List<OutboundCalls> outboundCallsList = outboundCallsPage.getContent();

		int callCountPointer = 0;
		if (outboundCallsList != null && outboundCallsList.size() > 0) {
			for (OutboundCalls outboundCalls : outboundCallsList) {
				try {
					outboundCalls.setAllocationStatus("allocated");
					outboundCalls.setAllocatedUserId(
							callAllocationDto.getToUserIds()[callCountPointer / callAllocationDto.getNoOfCalls()]);
					outboundCalls.setCallAttemptNo(0);

					callCountPointer++;
				} catch (Exception e) {
					// TODO: handle exception
					callCountPointer++;
				}
			}

			outboundCallsRepo.saveAll(outboundCallsList);

			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("response",
					outboundCallsList.size() + " mother record allocated successfully to selected MO");
			return new Gson().toJson(responseMap);
		} else
			throw new ECDException("no eligible record available to allocate, please contact administrator");

	}

	private String allocateChildRecordsToMO(RequestCallAllocationDTO callAllocationDto) throws ParseException {
		int totalRecordToAllocate = 0;

		if (callAllocationDto != null && callAllocationDto.getToUserIds() != null
				&& callAllocationDto.getToUserIds().length > 0 && callAllocationDto.getNoOfCalls() > 0)
			totalRecordToAllocate = (callAllocationDto.getToUserIds().length) * (callAllocationDto.getNoOfCalls());
		else
			throw new InvalidRequestException("please pass valid users");

		Timestamp tempFDateStamp = null;
		Timestamp tempTDateStamp = null;
		if (callAllocationDto.getFDate() != null && callAllocationDto.getTDate() != null) {
			tempFDateStamp = getTimestampFromString(
					callAllocationDto.getFDate().split("T")[0].concat("T00:00:00+05:30"));
			tempTDateStamp = getTimestampFromString(
					callAllocationDto.getTDate().split("T")[0].concat("T23:59:59+05:30"));
		} else
			throw new InvalidRequestException("from date / to date is null");

		Pageable pageable = PageRequest.of(0, totalRecordToAllocate);
		Page<OutboundCalls> outboundCallsPage = outboundCallsRepo.getChildRecordsForMO(pageable, "unallocated",
				callAllocationDto.getPsmId(), tempFDateStamp, tempTDateStamp);

		List<OutboundCalls> outboundCallsList = outboundCallsPage.getContent();

		int callCountPointer = 0;
		if (outboundCallsList != null && outboundCallsList.size() > 0) {
			for (OutboundCalls outboundCalls : outboundCallsList) {
				try {
					outboundCalls.setAllocationStatus("allocated");
					outboundCalls.setAllocatedUserId(
							callAllocationDto.getToUserIds()[callCountPointer / callAllocationDto.getNoOfCalls()]);
					outboundCalls.setCallAttemptNo(0);
					callCountPointer++;
				} catch (Exception e) {
					// TODO: handle exception
					callCountPointer++;
				}
			}

			outboundCallsRepo.saveAll(outboundCallsList);

			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("response",
					outboundCallsList.size() + " child record allocated successfully to selected MO");
			return new Gson().toJson(responseMap);
		} else
			throw new ECDException("no eligible record available to allocate, please contact administrator");

	}

	public ResponseEligibleCallRecordsDTO getEligibleRecordsInfo(int psmId, String phoneNoType, String recordType,
			String fDate, String tDate) {

		try {

			Timestamp tempFDateStamp = null;
			Timestamp tempTDateStamp = null;
			if (fDate != null && tDate != null) {
				tempFDateStamp = getTimestampFromString(fDate.split("T")[0].concat("T00:00:00+05:30"));
				tempTDateStamp = getTimestampFromString(tDate.split("T")[0].concat("T23:59:59+05:30"));
			} else
				throw new InvalidRequestException("from date / to date is null");

			ResponseEligibleCallRecordsDTO responseEligibleCallRecordsDTO = new ResponseEligibleCallRecordsDTO();
			int totalIntroductoryRecord = 0;
			int totalLowRisk = 0;
			int totalHighRisk = 0;
			int totalAllocated = 0;

			if (recordType != null && recordType.equalsIgnoreCase("Mother")) {

				totalIntroductoryRecord = motherRecordRepo.getRecordCount(false, tempFDateStamp, tempTDateStamp,
						phoneNoType);

				totalLowRisk = outboundCallsRepo.getMotherUnAllocatedCountLR("unallocated", psmId, tempFDateStamp,
						tempTDateStamp, phoneNoType);
				totalHighRisk = outboundCallsRepo.getMotherUnAllocatedCountHR("unallocated", psmId, tempFDateStamp,
						tempTDateStamp, phoneNoType);

				totalAllocated = outboundCallsRepo.getTotalAllocatedCountMother("allocated", psmId, tempFDateStamp,
						tempTDateStamp, phoneNoType);

			} else if (recordType != null && recordType.equalsIgnoreCase("Child")) {

				totalIntroductoryRecord = childRecordRepo.getRecordCount(false, tempFDateStamp, tempTDateStamp,
						phoneNoType);

				totalLowRisk = outboundCallsRepo.getChildUnAllocatedCountLR("unallocated", psmId, tempFDateStamp,
						tempTDateStamp, phoneNoType);
				totalHighRisk = outboundCallsRepo.getChildUnAllocatedCountHR("unallocated", psmId, tempFDateStamp,
						tempTDateStamp, phoneNoType);

				totalAllocated = outboundCallsRepo.getTotalAllocatedCountChild("allocated", psmId, tempFDateStamp,
						tempTDateStamp, phoneNoType);

			}

			responseEligibleCallRecordsDTO.setTotalIntroductoryRecord(totalIntroductoryRecord);
			responseEligibleCallRecordsDTO.setTotalLowRiskRecord(totalLowRisk);
			responseEligibleCallRecordsDTO.setTotalHighRiskRecord(totalHighRisk);
			responseEligibleCallRecordsDTO.setTotalRecord(totalIntroductoryRecord + totalLowRisk + totalHighRisk);

			responseEligibleCallRecordsDTO.setTotalAllocatedRecord(totalAllocated);

			return responseEligibleCallRecordsDTO;
		} catch (Exception e) {
			throw new ECDException(e);
		}

	}

	@Transactional(rollbackOn = Exception.class)
	public String moveAllocatedCallsToBin(RequestCallAllocationDTO callAllocationDto) {
		try {
			if (callAllocationDto.getUserId() != null && callAllocationDto.getNoOfCalls() != null
					&& callAllocationDto.getRecordType() != null && callAllocationDto.getPhoneNoType() != null
					&& callAllocationDto.getFDate() != null && callAllocationDto.getTDate() != null) {

				Timestamp tempFDateStamp = null;
				Timestamp tempTDateStamp = null;
				if (callAllocationDto.getFDate() != null && callAllocationDto.getTDate() != null) {
					tempFDateStamp = getTimestampFromString(
							callAllocationDto.getFDate().split("T")[0].concat("T00:00:00+05:30"));
					tempTDateStamp = getTimestampFromString(
							callAllocationDto.getTDate().split("T")[0].concat("T23:59:59+05:30"));
				} else
					throw new InvalidRequestException("from date / to date is null");

				Pageable pageable = PageRequest.of(0, callAllocationDto.getNoOfCalls());

				Page<OutboundCalls> outboundCallsPage = null;

				if (callAllocationDto.getRecordType().equalsIgnoreCase("Mother")) {

					outboundCallsPage = outboundCallsRepo.getAllocatedRecordsUserByRecordTypeAndPhoneTypeMother(
							pageable, callAllocationDto.getUserId(), "open", callAllocationDto.getPhoneNoType(),
							tempFDateStamp, tempTDateStamp);
				} else if (callAllocationDto.getRecordType().equalsIgnoreCase("Child")) {
					outboundCallsPage = outboundCallsRepo.getAllocatedRecordsUserByRecordTypeAndPhoneTypeChild(pageable,
							callAllocationDto.getUserId(), "open", callAllocationDto.getPhoneNoType(), tempFDateStamp,
							tempTDateStamp);
				}

				List<Long> motherIds = new ArrayList<>();
				List<Long> childIds = new ArrayList<>();

				if (outboundCallsPage != null && outboundCallsPage.getSize() > 0) {
					List<OutboundCalls> resultList = outboundCallsPage.getContent();
					for (OutboundCalls outboundCalls : resultList) {

						outboundCalls.setAllocatedUserId(null);
						outboundCalls.setAllocationStatus("unallocated");

						if (outboundCalls.getEcdCallType() != null
								&& outboundCalls.getEcdCallType().equalsIgnoreCase("introductory")) {
							outboundCalls.setDeleted(true);
							// write logic to update in mother or child table also - isAllocated = false
							if (outboundCalls.getChildId() != null)
								childIds.add(outboundCalls.getChildId());
							else if (outboundCalls.getMotherId() != null)
								motherIds.add(outboundCalls.getMotherId());

						}

					}

					outboundCallsRepo.saveAll(resultList);

					if (motherIds.size() > 0)
						motherRecordRepo.updateIsAllocatedFalse(motherIds);
					if (childIds.size() > 0)
						childRecordRepo.updateIsAllocatedFalse(childIds);
				} else
					throw new ECDException("no record available for move to bin. please contact administrator");
			} else
				throw new InvalidRequestException(callAllocationDto.toString(),
						"NULL or part of required request is NULL");

			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("response", "records successfully moved to bin");
			return new Gson().toJson(responseMap);
		} catch (Exception e) {
			throw new ECDException(e);
		}

	}

	public String getAllocatedCallCountUser(RequestCallAllocationDTO callAllocationDto) {
		Map<String, Integer> responseMap = new HashMap<>();
		try {
			if (callAllocationDto != null && callAllocationDto.getRecordType() != null) {

				Timestamp tempFDateStamp = null;
				Timestamp tempTDateStamp = null;
				if (callAllocationDto.getFDate() != null && callAllocationDto.getTDate() != null) {
					tempFDateStamp = getTimestampFromString(
							callAllocationDto.getFDate().split("T")[0].concat("T00:00:00+05:30"));
					tempTDateStamp = getTimestampFromString(
							callAllocationDto.getTDate().split("T")[0].concat("T23:59:59+05:30"));
				} else
					throw new InvalidRequestException("from date / to date is null");

				int cnt = 0;

				if (callAllocationDto.getRecordType().equalsIgnoreCase("Mother")) {
					cnt = outboundCallsRepo.getAllocatedRecordsCountMotherUser(callAllocationDto.getUserId(),
							tempFDateStamp, tempTDateStamp, "open", callAllocationDto.getPhoneNoType());
				} else if (callAllocationDto.getRecordType().equalsIgnoreCase("Child")) {
					cnt = outboundCallsRepo.getAllocatedRecordsCountChildUser(callAllocationDto.getUserId(),
							tempFDateStamp, tempTDateStamp, "open", callAllocationDto.getPhoneNoType());
				} else
					throw new InvalidRequestException("Invalid recordType",
							"please pass valid recordType - Mother / Child");

				responseMap.put("totalCount", cnt);

			} else
				throw new InvalidRequestException("NULL recordType", "please pass valid data/recordType");
			return new Gson().toJson(responseMap);
		} catch (Exception e) {
			throw new ECDException(e);
		}
	}

	@Transactional(rollbackOn = Exception.class)
	public String reAllocateCalls(RequestCallAllocationDTO callAllocationDto) {
		try {
			if (callAllocationDto != null && callAllocationDto.getUserId() != null
					&& callAllocationDto.getToUserIds() != null && callAllocationDto.getToUserIds().length > 0
					&& callAllocationDto.getRecordType() != null && callAllocationDto.getPhoneNoType() != null
					&& callAllocationDto.getNoOfCalls() != null) {

				int totalRecordToAllocate = 0;

				Timestamp tempFDateStamp = null;
				Timestamp tempTDateStamp = null;
				if (callAllocationDto.getFDate() != null && callAllocationDto.getTDate() != null) {
					tempFDateStamp = getTimestampFromString(
							callAllocationDto.getFDate().split("T")[0].concat("T00:00:00+05:30"));
					tempTDateStamp = getTimestampFromString(
							callAllocationDto.getTDate().split("T")[0].concat("T23:59:59+05:30"));
				} else
					throw new InvalidRequestException("from date / to date is null");

				if (callAllocationDto.getNoOfCalls() > 0)
					totalRecordToAllocate = (callAllocationDto.getToUserIds().length)
							* (callAllocationDto.getNoOfCalls());
				else
					throw new InvalidRequestException("NULL or Invalid NoOfCalls",
							"pass valid NoOfCalls, greater than 0");

				Pageable pageable = PageRequest.of(0, totalRecordToAllocate);

				Page<OutboundCalls> outboundCallsPage = null;

				if (callAllocationDto.getRecordType().equalsIgnoreCase("Mother")) {

					outboundCallsPage = outboundCallsRepo.getAllocatedRecordsUserByRecordTypeAndPhoneTypeMother(
							pageable, callAllocationDto.getUserId(), "open", callAllocationDto.getPhoneNoType(),
							tempFDateStamp, tempTDateStamp);
				} else if (callAllocationDto.getRecordType().equalsIgnoreCase("Child")) {
					outboundCallsPage = outboundCallsRepo.getAllocatedRecordsUserByRecordTypeAndPhoneTypeChild(pageable,
							callAllocationDto.getUserId(), "open", callAllocationDto.getPhoneNoType(), tempFDateStamp,
							tempTDateStamp);
				}

				if (outboundCallsPage != null && outboundCallsPage.getSize() > 0) {
					List<OutboundCalls> outboundCallsList = outboundCallsPage.getContent();

					int callCountPointer = 0;
					if (outboundCallsList != null && outboundCallsList.size() > 0) {
						for (OutboundCalls outboundCalls : outboundCallsList) {
							try {
								outboundCalls.setAllocationStatus("allocated");
								outboundCalls.setAllocatedUserId(callAllocationDto.getToUserIds()[callCountPointer
										/ callAllocationDto.getNoOfCalls()]);

								callCountPointer++;
							} catch (Exception e) {
								// TODO: handle exception
								callCountPointer++;
							}
						}

						outboundCallsRepo.saveAll(outboundCallsList);
					}
				}

			} else
				throw new InvalidRequestException("NULL userId/recordType/phoneNoType/noOfCalls/toUserIds",
						"pass valid userId-recordType-phoneNoType-noOfCalls-toUserIds");

			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("response", "records successfully re-allocated");
			return new Gson().toJson(responseMap);
		} catch (Exception e) {
			throw new ECDException(e);
		}

	}

	private Timestamp getTimestampFromString(String date) throws ParseException {

		DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;
		LocalDateTime localDateTime = LocalDateTime.from(ISO_DATE_TIME.parse(date));

		Timestamp timestamp = Timestamp.valueOf(localDateTime);

		return timestamp;

	}

	private Timestamp getTimestampDaysLater(Timestamp timeStamp, int daysLater) throws ParseException {
		if (timeStamp != null && daysLater >= 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(timeStamp);
			cal.add(Calendar.DAY_OF_WEEK, daysLater);

			return new Timestamp(cal.getTime().getTime());
		} else
			return new Timestamp(System.currentTimeMillis());
	}
}