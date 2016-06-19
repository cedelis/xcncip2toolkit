/**
 * Copyright (c) 2010 eXtensible Catalog Organization
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the MIT/X11 license. The text of the license can be
 * found at http://www.opensource.org/licenses/mit-license.php.
 */

package org.extensiblecatalog.ncip.v2.koha;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.extensiblecatalog.ncip.v2.ilsdiv1_1.ILSDIvOneOneLookupUserResponseData;
import org.extensiblecatalog.ncip.v2.ilsdiv1_1.LoanedItemsHistory;
import org.extensiblecatalog.ncip.v2.koha.util.KohaException;
import org.extensiblecatalog.ncip.v2.koha.util.KohaRemoteServiceManager;
import org.extensiblecatalog.ncip.v2.koha.util.KohaUtil;
import org.extensiblecatalog.ncip.v2.koha.util.LocalConfig;
import org.extensiblecatalog.ncip.v2.service.AccountDetails;
import org.extensiblecatalog.ncip.v2.service.AgencyId;
import org.extensiblecatalog.ncip.v2.service.AgencyUserPrivilegeType;
import org.extensiblecatalog.ncip.v2.service.AuthenticationInput;
import org.extensiblecatalog.ncip.v2.service.BlockOrTrap;
import org.extensiblecatalog.ncip.v2.service.ILSDIvOneOneLookupUserInitiationData;
import org.extensiblecatalog.ncip.v2.service.LoanedItem;
import org.extensiblecatalog.ncip.v2.service.LookupUserResponseData;
import org.extensiblecatalog.ncip.v2.service.NameInformation;
import org.extensiblecatalog.ncip.v2.service.PersonalNameInformation;
import org.extensiblecatalog.ncip.v2.service.Problem;
import org.extensiblecatalog.ncip.v2.service.ProblemType;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.RequestedItem;
import org.extensiblecatalog.ncip.v2.service.ResponseHeader;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.ServiceException;
import org.extensiblecatalog.ncip.v2.service.StructuredPersonalUserName;
import org.extensiblecatalog.ncip.v2.service.UserFiscalAccount;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.extensiblecatalog.ncip.v2.service.UserOptionalFields;
import org.extensiblecatalog.ncip.v2.service.UserPrivilege;
import org.extensiblecatalog.ncip.v2.service.Version1AuthenticationInputType;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;

public class KohaLookupUserService implements org.extensiblecatalog.ncip.v2.ilsdiv1_1.ILSDIv1_1_LookupUserService {

	@Override
	public ILSDIvOneOneLookupUserResponseData performService(ILSDIvOneOneLookupUserInitiationData initData,
			ServiceContext serviceContext, RemoteServiceManager serviceManager) throws ServiceException {

		final ILSDIvOneOneLookupUserResponseData responseData = new ILSDIvOneOneLookupUserResponseData();

		KohaRemoteServiceManager kohaRemoteServiceManager = (KohaRemoteServiceManager) serviceManager;

		try {

			boolean authenticated = false;
			if (initData.getAuthenticationInputs() != null && initData.getAuthenticationInputs().size() > 1) {
				processAuthInput(initData, kohaRemoteServiceManager, responseData);
				authenticated = true;
			}

			if (desiredAnything(initData)) {
				if (initData.getUserId() == null || initData.getUserId().getUserIdentifierValue().trim().isEmpty()) {
					responseData.setProblems(Arrays.asList(new Problem(new ProblemType("UserId is undefined."), null,
							null, "Cannot lookup unkown user ..")));
				} else {
					JSONObject kohaUser = kohaRemoteServiceManager.lookupUser(initData);
					updateResponseData(initData, responseData, kohaUser, kohaRemoteServiceManager);
				}
			} else if (authenticated)
				responseData.setUserId(initData.getUserId());
			else
				throw KohaException.create400BadRequestException("You have not desired anything processable");

		} catch (MalformedURLException mue) {
			Problem p = new Problem(new ProblemType("Processing MalformedURLException error."), null, mue.getMessage());
			responseData.setProblems(Arrays.asList(p));
		} catch (IOException ie) {
			Problem p = new Problem(new ProblemType("Processing IOException error."), ie.getMessage(),
					"Are you connected to the Internet/Intranet?");
			responseData.setProblems(Arrays.asList(p));
		} catch (SAXException se) {
			Problem p = new Problem(new ProblemType("Processing SAXException error."), null, se.getMessage());
			responseData.setProblems(Arrays.asList(p));
		} catch (KohaException ke) {
			Problem p = new Problem(new ProblemType(ke.getShortMessage()), null, ke.getMessage());
			responseData.setProblems(Arrays.asList(p));
		} catch (Exception e) {
			Problem p = new Problem(new ProblemType("Unknown processing exception error."), null,
					StringUtils.join(e.getStackTrace(), "\n"));
			responseData.setProblems(Arrays.asList(p));

		}
		return responseData;
	}

	private boolean desiredAnything(ILSDIvOneOneLookupUserInitiationData initData) {
		return initData.getBlockOrTrapDesired() || initData.getDateOfBirthDesired() || initData.getLoanedItemsDesired()
				|| initData.getNameInformationDesired() || initData.getRequestedItemsDesired()
				|| initData.getUserAddressInformationDesired() || initData.getUserFiscalAccountDesired()
				|| initData.getUserPrivilegeDesired() || initData.getHistoryDesired() != null;
	}

	private void processAuthInput(ILSDIvOneOneLookupUserInitiationData initData,
			KohaRemoteServiceManager kohaRemoteServiceManager, LookupUserResponseData responseData)
			throws KohaException, MalformedURLException, IOException, SAXException, URISyntaxException,
			org.json.simple.parser.ParseException {
		String userId = null, pass = null;
		for (AuthenticationInput input : initData.getAuthenticationInputs()) {
			if (input.getAuthenticationInputType().getValue()
					.equalsIgnoreCase(Version1AuthenticationInputType.USER_ID.getValue())) {
				userId = input.getAuthenticationInputData().trim();
			} else if (input.getAuthenticationInputType().getValue()
					.equalsIgnoreCase(Version1AuthenticationInputType.PASSWORD.getValue())) {
				pass = input.getAuthenticationInputData().trim();
			}
		}

		if (userId == null || pass == null || userId.isEmpty() || pass.isEmpty())
			throw new KohaException(KohaException.INVALID_AUTHENTICATIONINPUT_FORMAT,
					"User Id or Password are empty or not specified.");

		/*
		 * Will throw Exception if something is wrong
		 */
		JSONObject response = kohaRemoteServiceManager.authenticateUser(userId, pass);

		String authorized = (String) response.get("authorized");

		if (authorized == null || !authorized.equals("y"))
			throw new KohaException(KohaException.INVALID_CREDENTIALS_PROVIDED,
					"ILS could not authorize provided credentials - either User Id or Password is wrong");

		String userIdVal = (String) response.get("borNo");
		if (userIdVal == null || userIdVal.trim().isEmpty()) {
			try {
				// We need to verify userId is an integer all the time - if it
				// is, then we don't need userIdVal from Koha ILS - we actually
				// supplied it
				userIdVal = String.valueOf((Integer.parseInt(userId)));
			} catch (NumberFormatException e) {
				// If it's thrown, that mean userId is an string & Koha ILS
				// unexpectedly returned successful authentication
				throw KohaException.create400BadRequestException(
						"Sorry, Koha ILS authenticated user succesfully, but did not return borrowernumber.");
			}
		}
		// Setting user id means authentication was successful - otherwise there
		// should be thrown an Problem element instead
		initData.setUserId(KohaUtil.createUserId(userIdVal, LocalConfig.getDefaultAgency()));
	}

	private void updateResponseData(ILSDIvOneOneLookupUserInitiationData initData,
			ILSDIvOneOneLookupUserResponseData responseData, JSONObject kohaUser, KohaRemoteServiceManager svcMgr)
			throws ParseException, KohaException {

		ResponseHeader responseHeader = KohaUtil.reverseInitiationHeader(initData);

		if (responseHeader != null)
			responseData.setResponseHeader(responseHeader);

		UserId userId = KohaUtil.createUserId(initData.getUserId().getUserIdentifierValue(),
				LocalConfig.getDefaultAgency());
		responseData.setUserId(userId);

		UserOptionalFields userOptionalFields = new UserOptionalFields();

		if (LocalConfig.useRestApiInsteadOfSvc()) {

			if (initData.getNameInformationDesired()) {
				String firstname = (String) kohaUser.get("firstname");
				String surname = (String) kohaUser.get("surname");
				String title = (String) kohaUser.get("title");
				String othernames = (String) kohaUser.get("othernames");

				StructuredPersonalUserName structuredPersonalUserName = new StructuredPersonalUserName();
				structuredPersonalUserName.setGivenName(firstname);
				structuredPersonalUserName.setPrefix(title);
				structuredPersonalUserName.setSurname(surname);
				structuredPersonalUserName.setSuffix(othernames);

				PersonalNameInformation personalNameInformation = new PersonalNameInformation();
				personalNameInformation.setStructuredPersonalUserName(structuredPersonalUserName);

				NameInformation nameInformation = new NameInformation();
				nameInformation.setPersonalNameInformation(personalNameInformation);
				userOptionalFields.setNameInformation(nameInformation);
			}

			if (initData.getUserAddressInformationDesired())
				userOptionalFields.setUserAddressInformations(KohaUtil.parseUserAddressInformations(kohaUser));

			if (initData.getUserPrivilegeDesired()) {

				String branchcode = (String) kohaUser.get("branchcode");
				String agencyUserPrivilegeType = (String) kohaUser.get("categorycode");

				if (branchcode != null && agencyUserPrivilegeType != null) {

					List<UserPrivilege> userPrivileges = new ArrayList<UserPrivilege>();
					UserPrivilege userPrivilege = new UserPrivilege();

					userPrivilege.setAgencyId(new AgencyId(branchcode));

					userPrivilege.setAgencyUserPrivilegeType(new AgencyUserPrivilegeType(
							"http://www.niso.org/ncip/v1_0/imp1/schemes/agencyuserprivilegetype/agencyuserprivilegetype.scm",
							agencyUserPrivilegeType));

					userPrivilege.setValidFromDate(
							KohaUtil.parseGregorianCalendarFromKohaDate((String) kohaUser.get("dateenrolled")));
					userPrivilege.setValidToDate(
							KohaUtil.parseGregorianCalendarFromKohaDate((String) kohaUser.get("dateexpiry")));

					userPrivileges.add(userPrivilege);
					userOptionalFields.setUserPrivileges(userPrivileges);
				}
			}

			if (initData.getDateOfBirthDesired()) {
				String dateOfBirth = (String) kohaUser.get("dateofbirth");
				userOptionalFields.setDateOfBirth(KohaUtil.parseGregorianCalendarFromKohaDate(dateOfBirth));
			}

			if (initData.getLoanedItemsDesired()) {
				JSONArray checkouts = (JSONArray) kohaUser.get("checkouts");

				if (checkouts != null && checkouts.size() != 0) {
					List<LoanedItem> loanedItems = new ArrayList<LoanedItem>();
					for (int i = 0; i < checkouts.size(); ++i) {
						JSONObject checkout = (JSONObject) checkouts.get(i);
						loanedItems.add(KohaUtil.parseLoanedItem(checkout));
					}
					responseData.setLoanedItems(loanedItems);
				}

			}

			if (initData.getRequestedItemsDesired()) {
				JSONArray holds = (JSONArray) kohaUser.get("holds");

				if (holds != null && holds.size() != 0) {
					List<RequestedItem> requestedItems = new ArrayList<RequestedItem>();
					for (int i = 0; i < holds.size(); ++i) {
						JSONObject hold = (JSONObject) holds.get(i);
						RequestedItem requestedItem = KohaUtil.parseRequestedItem(hold);
						if (requestedItem != null)
							requestedItems.add(requestedItem);
					}
					responseData.setRequestedItems(requestedItems);
				}
			}

			if (initData.getBlockOrTrapDesired()) {

				List<BlockOrTrap> blocks = new ArrayList<BlockOrTrap>();

				// Parse expiration
				GregorianCalendar expiryDate = KohaUtil
						.parseGregorianCalendarFromKohaDate((String) kohaUser.get("dateexpiry"));

				GregorianCalendar warningForExpiryDateGregCal = ((GregorianCalendar) expiryDate.clone());
				warningForExpiryDateGregCal.add(GregorianCalendar.DAY_OF_YEAR, -14);

				Date now = new Date();
				if (now.after(expiryDate.getTime())) {

					BlockOrTrap expired = KohaUtil.createBlockOrTrap("Expired");

					blocks.add(expired);

				} else if (now.after(warningForExpiryDateGregCal.getTime())) {

					Days days = Days.daysBetween(new DateTime(now), new DateTime(expiryDate));

					BlockOrTrap expiresSoon = KohaUtil
							.createBlockOrTrap("Expires in " + (days.getDays() + 1) + " days");

					blocks.add(expiresSoon);
				}

				// Parse debarred status
				String debarredStatus = (String) kohaUser.get("debarred");

				if (debarredStatus != null && !debarredStatus.isEmpty()) {

					String debarredComment = (String) kohaUser.get("debarredcomment");

					BlockOrTrap debarred = KohaUtil
							.createBlockOrTrap("Debarred" + (debarredComment != null ? ": " + debarredComment : ""));

					blocks.add(debarred);
				}

				if (blocks.size() > 0)
					userOptionalFields.setBlockOrTraps(blocks);

			}

			if (initData.getUserFiscalAccountDesired()) {
				
				JSONArray accountLines = (JSONArray) kohaUser.get("accountLines");
				
				if (accountLines != null && accountLines.size() != 0) {
					List<UserFiscalAccount> userFiscalAccounts = new ArrayList<UserFiscalAccount>();
					UserFiscalAccount userFiscalAccount = new UserFiscalAccount();
					List<AccountDetails> accountDetails = new ArrayList<AccountDetails>();

					for (int i = 0; i < accountLines.size(); ++i) {
						JSONObject accountLine = (JSONObject) accountLines.get(i);
						accountDetails.add(KohaUtil.parseAccountDetails(accountLine));
					}

					BigDecimal amount = null; // Sum all transactions ..
					for (AccountDetails details : accountDetails) {
						if (amount == null)
							amount = details.getFiscalTransactionInformation().getAmount().getMonetaryValue();
						else
							amount.add(details.getFiscalTransactionInformation().getAmount().getMonetaryValue());
					}
					userFiscalAccount.setAccountBalance(KohaUtil.createAccountBalance(amount));

					userFiscalAccount.setAccountDetails(accountDetails);
					userFiscalAccounts.add(userFiscalAccount); // Suppose user has
																// only one account
																// ..
					responseData.setUserFiscalAccounts(userFiscalAccounts);
				}
			}

			if (initData.getHistoryDesired() != null) {

				// FIXME
				LoanedItemsHistory loanedItemsHistory = KohaUtil.parseLoanedItemsHistory(kohaUser, initData);
				responseData.setLoanedItemsHistory(loanedItemsHistory);
			}

		} else {
			JSONObject userInfo = (JSONObject) kohaUser.get("userInfo");
			if (userInfo != null) {
				if (initData.getNameInformationDesired()) {
					String firstname = (String) userInfo.get("firstname");
					String surname = (String) userInfo.get("surname");
					String title = (String) userInfo.get("title");
					String othernames = (String) userInfo.get("othernames");

					StructuredPersonalUserName structuredPersonalUserName = new StructuredPersonalUserName();
					structuredPersonalUserName.setGivenName(firstname);
					structuredPersonalUserName.setPrefix(title);
					structuredPersonalUserName.setSurname(surname);
					structuredPersonalUserName.setSuffix(othernames);

					PersonalNameInformation personalNameInformation = new PersonalNameInformation();
					personalNameInformation.setStructuredPersonalUserName(structuredPersonalUserName);

					NameInformation nameInformation = new NameInformation();
					nameInformation.setPersonalNameInformation(personalNameInformation);
					userOptionalFields.setNameInformation(nameInformation);
				}

				if (initData.getUserAddressInformationDesired())
					userOptionalFields.setUserAddressInformations(KohaUtil.parseUserAddressInformations(userInfo));

				if (initData.getUserPrivilegeDesired()) {

					String branchcode = (String) userInfo.get("branchcode");
					String agencyUserPrivilegeType = (String) userInfo.get("categorycode");

					if (branchcode != null && agencyUserPrivilegeType != null) {

						List<UserPrivilege> userPrivileges = new ArrayList<UserPrivilege>();
						UserPrivilege userPrivilege = new UserPrivilege();

						userPrivilege.setAgencyId(new AgencyId(branchcode));

						userPrivilege.setAgencyUserPrivilegeType(new AgencyUserPrivilegeType(
								"http://www.niso.org/ncip/v1_0/imp1/schemes/agencyuserprivilegetype/agencyuserprivilegetype.scm",
								agencyUserPrivilegeType));

						userPrivilege.setValidFromDate(
								KohaUtil.parseGregorianCalendarFromKohaDate((String) userInfo.get("dateenrolled")));
						userPrivilege.setValidToDate(
								KohaUtil.parseGregorianCalendarFromKohaDate((String) userInfo.get("dateexpiry")));

						userPrivileges.add(userPrivilege);
						userOptionalFields.setUserPrivileges(userPrivileges);
					}
				}

				if (initData.getBlockOrTrapDesired()) {
					List<BlockOrTrap> blockOrTraps = KohaUtil.parseBlockOrTraps((JSONArray) userInfo.get("blocks"));
					userOptionalFields.setBlockOrTraps(blockOrTraps);
				}

				if (initData.getDateOfBirthDesired()) {
					String dateOfBirth = (String) userInfo.get("dateofbirth");
					userOptionalFields.setDateOfBirth(KohaUtil.parseGregorianCalendarFromKohaDate(dateOfBirth));
				}
			}

			JSONArray requestedItemsParsed = (JSONArray) kohaUser.get("requestedItems");
			if (requestedItemsParsed != null && requestedItemsParsed.size() != 0) {
				List<RequestedItem> requestedItems = new ArrayList<RequestedItem>();
				for (int i = 0; i < requestedItemsParsed.size(); ++i) {
					JSONObject requestedItemParsed = (JSONObject) requestedItemsParsed.get(i);
					RequestedItem requestedItem = KohaUtil.parseRequestedItem(requestedItemParsed);
					if (requestedItem != null)
						requestedItems.add(requestedItem);
				}
				responseData.setRequestedItems(requestedItems);
			}

			JSONArray loanedItemsParsed = (JSONArray) kohaUser.get("loanedItems");
			if (loanedItemsParsed != null && loanedItemsParsed.size() != 0) {
				List<LoanedItem> loanedItems = new ArrayList<LoanedItem>();
				for (int i = 0; i < loanedItemsParsed.size(); ++i) {
					JSONObject loanedItem = (JSONObject) loanedItemsParsed.get(i);
					loanedItems.add(KohaUtil.parseLoanedItem(loanedItem));
				}
				responseData.setLoanedItems(loanedItems);
			}

			JSONArray userFiscalAccountParsed = (JSONArray) kohaUser.get("userFiscalAccount");
			if (userFiscalAccountParsed != null && userFiscalAccountParsed.size() != 0) {
				List<UserFiscalAccount> userFiscalAccounts = new ArrayList<UserFiscalAccount>();
				UserFiscalAccount userFiscalAccount = new UserFiscalAccount();
				List<AccountDetails> accountDetails = new ArrayList<AccountDetails>();

				for (int i = 0; i < userFiscalAccountParsed.size(); ++i) {
					JSONObject accountDetail = (JSONObject) userFiscalAccountParsed.get(i);
					accountDetails.add(KohaUtil.parseAccountDetails(accountDetail));
				}

				BigDecimal amount = null; // Sum all transactions ..
				for (AccountDetails details : accountDetails) {
					if (amount == null)
						amount = details.getFiscalTransactionInformation().getAmount().getMonetaryValue();
					else
						amount.add(details.getFiscalTransactionInformation().getAmount().getMonetaryValue());
				}
				userFiscalAccount.setAccountBalance(KohaUtil.createAccountBalance(amount));

				userFiscalAccount.setAccountDetails(accountDetails);
				userFiscalAccounts.add(userFiscalAccount); // Suppose user has
															// only one account
															// ..
				responseData.setUserFiscalAccounts(userFiscalAccounts);
			}

		}
		responseData.setUserOptionalFields(userOptionalFields);

	}
}
