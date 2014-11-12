package org.extensiblecatalog.ncip.v2.aleph;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.aleph.restdlf.AlephConstants;
import org.extensiblecatalog.ncip.v2.aleph.restdlf.AlephException;
import org.extensiblecatalog.ncip.v2.aleph.util.AlephRemoteServiceManager;
import org.extensiblecatalog.ncip.v2.aleph.restdlf.item.AlephItem;
import org.extensiblecatalog.ncip.v2.aleph.util.AlephUtil;
import org.extensiblecatalog.ncip.v2.aleph.util.ItemToken;
import org.extensiblecatalog.ncip.v2.service.AgencyId;
import org.extensiblecatalog.ncip.v2.service.BibInformation;
import org.extensiblecatalog.ncip.v2.service.BibliographicDescription;
import org.extensiblecatalog.ncip.v2.service.BibliographicId;
import org.extensiblecatalog.ncip.v2.service.BibliographicItemId;
import org.extensiblecatalog.ncip.v2.service.BibliographicItemIdentifierCode;
import org.extensiblecatalog.ncip.v2.service.BibliographicRecordIdentifierCode;
import org.extensiblecatalog.ncip.v2.service.HoldingsInformation;
import org.extensiblecatalog.ncip.v2.service.HoldingsSet;
import org.extensiblecatalog.ncip.v2.service.ItemDescription;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.ItemInformation;
import org.extensiblecatalog.ncip.v2.service.ItemOptionalFields;
import org.extensiblecatalog.ncip.v2.service.ItemUseRestrictionType;
import org.extensiblecatalog.ncip.v2.service.LookupItemSetInitiationData;
import org.extensiblecatalog.ncip.v2.service.LookupItemSetResponseData;
import org.extensiblecatalog.ncip.v2.service.LookupItemSetService;
import org.extensiblecatalog.ncip.v2.service.Problem;
import org.extensiblecatalog.ncip.v2.service.ProblemType;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.ServiceError;
import org.extensiblecatalog.ncip.v2.service.ServiceException;
import org.extensiblecatalog.ncip.v2.service.ServiceHelper;
import org.extensiblecatalog.ncip.v2.service.Version1BibliographicItemIdentifierCode;
import org.extensiblecatalog.ncip.v2.service.Version1BibliographicRecordIdentifierCode;
import org.extensiblecatalog.ncip.v2.service.Version1GeneralProcessingError;
import org.extensiblecatalog.ncip.v2.service.Version1ItemDescriptionLevel;
import org.extensiblecatalog.ncip.v2.service.Version1ItemIdentifierType;
import org.extensiblecatalog.ncip.v2.service.Version1ItemUseRestrictionType;
import org.extensiblecatalog.ncip.v2.service.Version1LookupItemProcessingError;
import org.xml.sax.SAXException;

public class AlephLookupItemSetService implements LookupItemSetService {

	static Logger log = Logger.getLogger(AlephLookupItemSetService.class);

	private static HashMap<String, ItemToken> tokens = new HashMap<String, ItemToken>();

	private int itemsForwarded;
	private int maximumItemsCount;

	private Random random = new Random();

	/**
	 * Construct an AlephRemoteServiceManager; this class is not configurable so there are no parameters.
	 */
	public AlephLookupItemSetService() {
	}

	@Override
	public LookupItemSetResponseData performService(LookupItemSetInitiationData initData, ServiceContext serviceContext, RemoteServiceManager serviceManager)
			throws ServiceException {
		Date sService = new Date();
		LookupItemSetResponseData responseData = new LookupItemSetResponseData();
		AlephRemoteServiceManager alephSvcMgr = (AlephRemoteServiceManager) serviceManager;

		itemsForwarded = 0;
		maximumItemsCount = 0;
		if (initData.getMaximumItemsCount() != null)
			try {
				maximumItemsCount = Integer.parseInt(initData.getMaximumItemsCount().toString());
			} catch (Exception e) {
				Problem problem = new Problem(new ProblemType("Maximum items count is not in parseable format."), null, e.getMessage());
				responseData.setProblems(Arrays.asList(problem));
				return responseData;
			}

		List<BibliographicId> bibIds = initData.getBibliographicIds();

		String token = initData.getNextItemToken();
		ItemToken nextItemToken = null;
		// remove any bib ids from bibIds list that may have already been processed
		if (token != null) {
			// In case maximum items returned was reached while parsing one bibliographic Id with a lot alephItems inside
			nextItemToken = tokens.get(token);
			if (nextItemToken != null) {
				int index = getBibIdIndex(bibIds, nextItemToken.getBibliographicId());
				if (index != -1) {
					// remove the ones already processed
					if (nextItemToken.isRecordId() && !nextItemToken.doneWithRecordId())
						bibIds.subList(0, index).clear();
					else
						bibIds.subList(0, ++index).clear();
				}

				// Remove token from memory hashmap
				tokens.remove(token);
			} else {
				Problem problem = new Problem(new ProblemType("Invalid NextItemToken"), null, "Recieved token: " + token);
				responseData.setProblems(Arrays.asList(problem));
				return responseData;
			}
			log.debug("after removing already processed Bib ids =" + bibIds);

		}

		boolean getCurrentBorrower = initData.getCurrentBorrowerDesired();
		boolean getCurrentRequesters = initData.getCurrentRequestersDesired();
		boolean getItemUseRestrictionType = initData.getItemUseRestrictionTypeDesired();
		boolean getPhysicalCondition = initData.getPhysicalConditionDesired();
		boolean getSecurityMarker = initData.getSecurityMarkerDesired();
		boolean getSensitizationFlag = initData.getSensitizationFlagDesired();
		boolean getElectronicResource = initData.getElectronicResourceDesired();
		boolean getLocation = initData.getLocationDesired();

		boolean getBibDescription = initData.getBibliographicDescriptionDesired();
		boolean getCircStatus = initData.getCirculationStatusDesired();
		boolean getHoldQueueLength = initData.getHoldQueueLengthDesired();
		boolean getItemDescription = initData.getItemDescriptionDesired();

		List<BibInformation> bibInformations = new ArrayList<BibInformation>();

		if (bibIds != null && bibIds.size() > 0) {
			for (BibliographicId bibId : bibIds) {
				BibInformation bibInformation = new BibInformation();

				try {
					if (bibId.getBibliographicRecordId() != null) {

						String id = bibId.getBibliographicRecordId().getBibliographicRecordIdentifier();
						log.debug("Processing Bib id = " + id);

						bibInformation.setBibliographicId(bibId);

						List<AlephItem> alephItems = alephSvcMgr.lookupItems(id, getBibDescription, getCircStatus, getHoldQueueLength, getItemDescription);
						if (alephItems != null) {
							int foundPieces = alephItems.size();

							if (nextItemToken != null) {
								if (nextItemToken.isRecordId() && !nextItemToken.doneWithRecordId()) {
									alephItems.subList(0, nextItemToken.getNoOfDoneItemIds()).clear();
									nextItemToken.setDoneWithRecordId(true);
								}
							}

							for (AlephItem item : alephItems) {
								item.setNumberOfPieces(new BigDecimal(foundPieces));
							}

							AgencyId suppliedAgencyId;
							if (getBibDescription) {
								if (initData.getInitiationHeader() == null || initData.getInitiationHeader().getFromAgencyId() == null)
									suppliedAgencyId = alephSvcMgr.toAgencyId(alephSvcMgr.getDefaultAgencyId());
								else
									suppliedAgencyId = initData.getInitiationHeader().getFromAgencyId().getAgencyId();
							} else
								suppliedAgencyId = null;
							// TODO: move parseHoldingsSets method to AlephUtil & purge this horrible code so that it is more understandable
							List<HoldingsSet> holdingSets = parseHoldingsSets(alephItems, suppliedAgencyId, initData);

							bibInformation.setHoldingsSets(holdingSets);

							int itemsToForward = holdingSets.get(0).getItemInformations().size();
							if (itemsToForward > 0) {

								bibInformations.add(bibInformation);

								// It is important to let token create even if the bibId is last of all desired bibIds if not all items can be forwarded
								boolean isLast = bibIds.get(bibIds.size() - 1).equals(bibId) && itemsToForward == alephItems.size();

								// Do not create NextItemToken if this is last item desired
								if (maximumItemsCount == itemsForwarded && !isLast) {
									// Set next item token
									ItemToken itemToken = new ItemToken();

									itemToken.setBibliographicId(id);
									// itemToken.setItemId(alephItems.get(itemsCount - 1).getItemId());
									itemToken.setIsRecordId(true);

									if (itemsToForward == alephItems.size())
										itemToken.setDoneWithRecordId(true);
									else {
										if (nextItemToken == null)
											itemToken.setNoOfDoneItemIds(itemsToForward);
										else
											itemToken.setNoOfDoneItemIds(itemsToForward + nextItemToken.getNoOfDoneItemIds());
									}

									int newToken = random.nextInt();

									itemToken.setNextToken(Integer.toString(newToken));
									tokens.put(Integer.toString(newToken), itemToken);

									responseData.setNextItemToken(Integer.toString(newToken));
									break;
								}
							}

						} else if (alephSvcMgr.echoParticularProblemsToLUIS) {
							Problem p = new Problem();
							p.setProblemType(Version1LookupItemProcessingError.UNKNOWN_ITEM);
							p.setProblemDetail("Item " + id + ", you are searching for, does not exists.");

							List<Problem> problems = new ArrayList<Problem>();
							problems.add(p);

							bibInformation.setProblems(Arrays.asList(p));

							bibInformations.add(bibInformation);
							itemsForwarded++;

							boolean isLast = bibIds.get(bibIds.size() - 1).equals(bibId);

							// Do not create NextItemToken if this is last item desired
							if (maximumItemsCount == itemsForwarded && !isLast) {
								// Set next item token
								ItemToken itemToken = new ItemToken();

								itemToken.setBibliographicId(id);

								int newToken = random.nextInt();
								itemToken.setNextToken(Integer.toString(newToken));

								tokens.put(Integer.toString(newToken), itemToken);

								responseData.setNextItemToken(Integer.toString(newToken));
								break;
							}
						}

					} else if (bibId.getBibliographicItemId() != null) {
						String id = bibId.getBibliographicItemId().getBibliographicItemIdentifier();
						bibInformation.setBibliographicId(bibId);

						AlephItem alephItem = alephSvcMgr.lookupItem(id, getBibDescription, getCircStatus, getHoldQueueLength, getItemDescription);
						if (alephItem != null) {

							AgencyId suppliedAgencyId;
							if (getBibDescription) {
								if (initData.getInitiationHeader() == null || initData.getInitiationHeader().getFromAgencyId() == null)
									suppliedAgencyId = alephSvcMgr.toAgencyId(alephSvcMgr.getDefaultAgencyId());
								else
									suppliedAgencyId = initData.getInitiationHeader().getFromAgencyId().getAgencyId();
							} else
								suppliedAgencyId = null;

							HoldingsSet holdingsSet = parseHoldingsSet(alephItem, suppliedAgencyId, initData);

							List<HoldingsSet> holdingsSets = new ArrayList<HoldingsSet>();
							holdingsSets.add(holdingsSet);
							bibInformation.setHoldingsSets(holdingsSets);

							bibInformations.add(bibInformation);
							itemsForwarded++;

							boolean isLast = bibIds.get(bibIds.size() - 1).equals(bibId);
							// Do not create NextItemToken if this is last item desired
							if (maximumItemsCount == itemsForwarded && !isLast) {

								ItemToken itemToken = new ItemToken();
								itemToken.setBibliographicId(id);
								itemToken.setItemId(alephItem.getItemId());

								int newToken = random.nextInt();

								itemToken.setNextToken(Integer.toString(newToken));
								tokens.put(Integer.toString(newToken), itemToken);

								responseData.setNextItemToken(Integer.toString(newToken));
								break;
							}
						} else if (alephSvcMgr.echoParticularProblemsToLUIS) {
							Problem p = new Problem();
							p.setProblemType(Version1LookupItemProcessingError.UNKNOWN_ITEM);
							p.setProblemDetail("Item " + id + ", you are searching for, does not exists.");

							List<Problem> problems = new ArrayList<Problem>();
							problems.add(p);

							bibInformation.setProblems(Arrays.asList(p));

							// Note that Problem elements within <ns1:BibInformation> is also considered as one item forwarded
							bibInformations.add(bibInformation);
							itemsForwarded++;

							boolean isLast = bibIds.get(bibIds.size() - 1).equals(bibId);
							// Do not create NextItemToken if this is last item desired
							if (maximumItemsCount == itemsForwarded && !isLast) {

								ItemToken itemToken = new ItemToken();
								itemToken.setBibliographicId(id);

								int newToken = random.nextInt();

								itemToken.setNextToken(Integer.toString(newToken));
								tokens.put(Integer.toString(newToken), itemToken);

								responseData.setNextItemToken(Integer.toString(newToken));
								break;
							}
						}

					} else {

						bibInformation.setProblems(ServiceHelper.generateProblems(Version1GeneralProcessingError.NEEDED_DATA_MISSING, "BibliographicItemId/RecordId", null,
								"BibliographicItemId/RecordId was not properly set."));
						bibInformations.add(bibInformation);
					}
				} catch (IOException ie) {
					Problem p = new Problem(new ProblemType("Processing IOException error."), null, ie.getMessage());
					bibInformation.setProblems(Arrays.asList(p));

					bibInformations.add(bibInformation);
				} catch (ParserConfigurationException pce) {
					Problem p = new Problem(new ProblemType("Processing ParserConfigurationException error."), null, pce.getMessage());
					bibInformation.setProblems(Arrays.asList(p));

					bibInformations.add(bibInformation);
				} catch (SAXException se) {
					Problem p = new Problem(new ProblemType("Processing SAXException error."), null, se.getMessage());
					bibInformation.setProblems(Arrays.asList(p));

					bibInformations.add(bibInformation);
				} catch (AlephException ae) {
					Problem p = new Problem(new ProblemType("Processing AlephException error."), null, ae.getMessage());
					bibInformation.setProblems(Arrays.asList(p));

					bibInformations.add(bibInformation);
				} catch (Exception e) {
					Problem p = new Problem(new ProblemType("Unknown processing exception error."), null, e.getMessage());
					responseData.setProblems(Arrays.asList(p));
				}

			}

		} else if (initData.getItemIds() != null && initData.getItemIds().size() > 0) {

			for (ItemId itemId : initData.getItemIds()) {

				ItemInformation itemInformation = new ItemInformation();
				itemInformation.setProblems(ServiceHelper.generateProblems(Version1LookupItemProcessingError.UNKNOWN_ITEM, "//BibliographicRecordId",
						itemId.getItemIdentifierValue(), "Item # '" + itemId.getItemIdentifierValue() + "' not found."));
				List<ItemInformation> itemInformationList = new ArrayList<ItemInformation>();
				itemInformationList.add(itemInformation);
				HoldingsSet holdingsSet = new HoldingsSet();
				holdingsSet.setItemInformations(itemInformationList);
				List<HoldingsSet> holdingsSetList = new ArrayList<HoldingsSet>();
				BibInformation bibInformation = new BibInformation();
				bibInformation.setHoldingsSets(holdingsSetList);
				bibInformations.add(bibInformation);

			}

		} else {
			Problem problem = new Problem(new ProblemType("Unknown Problem"), null, "Toolkit passed through empty initiation data.");
			responseData.setProblems(Arrays.asList(problem));
			return responseData;
		}

		if (responseData.getProblems() == null || responseData.getProblem(0) == null)
			responseData.setBibInformations(bibInformations);

		Date eService = new Date();

		log.debug("Service time log : " + (eService.getTime() - sService.getTime()) + "  " + ((eService.getTime() - sService.getTime()) / 1000) + " sec");
		log.debug(responseData);
		log.debug("Tokens in memory: " + tokens);
		return responseData;
	}

	/**
	 * 
	 * Parses response from requested ItemId (we know for sure there is only one item to parse)
	 * 
	 * @param alephItem
	 * @param suppliedAgencyId
	 * @param getBibDescription
	 * @param getCircStatus
	 * @param getHoldQueueLength
	 * @param getItemDescription
	 * @return
	 */
	private HoldingsSet parseHoldingsSet(AlephItem alephItem, AgencyId suppliedAgencyId, LookupItemSetInitiationData initData) {
		HoldingsSet holdingsSet = new HoldingsSet();
		List<ItemInformation> itemInfoList = new ArrayList<ItemInformation>();

		ItemOptionalFields iof = new ItemOptionalFields();

		if (initData.getBibliographicDescriptionDesired()) {
			BibliographicDescription bDesc = AlephUtil.getBibliographicDescription(alephItem, suppliedAgencyId);
			holdingsSet.setBibliographicDescription(bDesc);
		}

		addItemIdentifierToItemOptionalFields(iof, alephItem.getBarcode(), Version1BibliographicItemIdentifierCode.LEGAL_DEPOSIT_NUMBER);

		ItemInformation info = new ItemInformation();

		ItemId itemId = new ItemId();
		itemId.setItemIdentifierValue(alephItem.getItemId());
		itemId.setItemIdentifierType(Version1ItemIdentifierType.ACCESSION_NUMBER);
		info.setItemId(itemId);

		if (initData.getHoldQueueLengthDesired())
			iof.setHoldQueueLength(new BigDecimal(alephItem.getHoldQueueLength()));
		if (initData.getCirculationStatusDesired())
			iof.setCirculationStatus(alephItem.getCirculationStatus());
		if (initData.getItemDescriptionDesired()) {
			ItemDescription itemDescription = new ItemDescription();
			itemDescription.setItemDescriptionLevel(Version1ItemDescriptionLevel.ITEM);
			itemDescription.setCallNumber(alephItem.getCallNumber());
			itemDescription.setCopyNumber(alephItem.getCopyNumber());
			itemDescription.setNumberOfPieces(alephItem.getNumberOfPieces());
			if (alephItem.getDescription() != null) {
				HoldingsInformation holdingsInformation = new HoldingsInformation();
				holdingsInformation.setUnstructuredHoldingsData(alephItem.getDescription());
				itemDescription.setHoldingsInformation(holdingsInformation);
			}
			iof.setItemDescription(itemDescription);
		}
		if (initData.getItemUseRestrictionTypeDesired()) {
			if (alephItem.getItemRestrictions().size() > 0) {

				List<ItemUseRestrictionType> itemUseRestrictionTypes = new ArrayList<ItemUseRestrictionType>();

				for (String itemRestriction : alephItem.getItemRestrictions()) {

					Version1ItemUseRestrictionType itemUseRestrictionTypeScheme = AlephUtil.parseItemUseRestrictionType(itemRestriction);

					ItemUseRestrictionType itemUseRestrictionType = itemUseRestrictionTypeScheme;

					if (itemUseRestrictionType != null)
						itemUseRestrictionTypes.add(itemUseRestrictionType);
				}

				if (itemUseRestrictionTypes.size() > 0)
					iof.setItemUseRestrictionTypes(itemUseRestrictionTypes);
			}
		}

		info.setItemOptionalFields(iof);
		itemInfoList.add(info);
		holdingsSet.setItemInformations(itemInfoList);

		return holdingsSet;
	}

	/**
	 * 
	 * Parses response from requested RecordId
	 * 
	 * @param alephItems
	 * @param suppliedAgencyId
	 * @param getBibDescription
	 * @param getCircStatus
	 * @param getHoldQueueLength
	 * @param getItemDescription
	 * @return
	 */
	private List<HoldingsSet> parseHoldingsSets(List<AlephItem> alephItems, AgencyId suppliedAgencyId, LookupItemSetInitiationData initData) {
		List<HoldingsSet> holdingsSets = new ArrayList<HoldingsSet>();
		HoldingsSet holdingsSet = new HoldingsSet();
		List<ItemInformation> itemInfoList = new ArrayList<ItemInformation>();
		for (AlephItem item : alephItems) {

			if (maximumItemsCount == 0 || itemsForwarded < maximumItemsCount) {
				ItemOptionalFields iof = new ItemOptionalFields();

				if (initData.getBibliographicDescriptionDesired()) {
					BibliographicDescription bDesc = AlephUtil.getBibliographicDescription(item, suppliedAgencyId);
					holdingsSet.setBibliographicDescription(bDesc);
				}

				addItemIdentifierToItemOptionalFields(iof, item.getBarcode(), Version1BibliographicItemIdentifierCode.LEGAL_DEPOSIT_NUMBER);

				ItemInformation info = new ItemInformation();

				ItemId itemId = new ItemId();
				itemId.setItemIdentifierValue(item.getItemId());
				itemId.setItemIdentifierType(Version1ItemIdentifierType.ACCESSION_NUMBER);
				info.setItemId(itemId);

				if (initData.getHoldQueueLengthDesired())
					iof.setHoldQueueLength(new BigDecimal(item.getHoldQueueLength()));
				if (initData.getCirculationStatusDesired())
					iof.setCirculationStatus(item.getCirculationStatus());
				if (initData.getItemDescriptionDesired()) {
					ItemDescription itemDescription = new ItemDescription();
					itemDescription.setItemDescriptionLevel(Version1ItemDescriptionLevel.ITEM);
					itemDescription.setCallNumber(item.getCallNumber());
					itemDescription.setCopyNumber(item.getCopyNumber());
					itemDescription.setNumberOfPieces(item.getNumberOfPieces());
					if (item.getDescription() != null) {
						HoldingsInformation holdingsInformation = new HoldingsInformation();
						holdingsInformation.setUnstructuredHoldingsData(item.getDescription());
						itemDescription.setHoldingsInformation(holdingsInformation);
					}
					iof.setItemDescription(itemDescription);
				}
				if (initData.getItemUseRestrictionTypeDesired()) {
					if (item.getItemRestrictions().size() > 0) {

						List<ItemUseRestrictionType> itemUseRestrictionTypes = new ArrayList<ItemUseRestrictionType>();

						for (String itemRestriction : item.getItemRestrictions()) {

							Version1ItemUseRestrictionType itemUseRestrictionTypeScheme = AlephUtil.parseItemUseRestrictionType(itemRestriction);

							ItemUseRestrictionType itemUseRestrictionType = itemUseRestrictionTypeScheme;

							if (itemUseRestrictionType != null)
								itemUseRestrictionTypes.add(itemUseRestrictionType);
						}

						if (itemUseRestrictionTypes.size() > 0)
							iof.setItemUseRestrictionTypes(itemUseRestrictionTypes);
					}
				}

				info.setItemOptionalFields(iof);

				itemInfoList.add(info);
				itemsForwarded++;
			} else
				break;
		}
		holdingsSet.setItemInformations(itemInfoList);

		holdingsSets.add(holdingsSet);
		return holdingsSets;
	}

	/**
	 * Adds passed String to Item Optional Fields as BibliographicItemIdentifier with passed BibliographicItemIdentifierCode.
	 * 
	 * @param iof
	 * @param barcode
	 * @param bibItemIdCode
	 */
	private void addItemIdentifierToItemOptionalFields(ItemOptionalFields iof, String barcode, BibliographicItemIdentifierCode bibItemIdCode) {
		BibliographicDescription bibDesc = new BibliographicDescription();
		List<BibliographicItemId> itemIds = new ArrayList<BibliographicItemId>();
		BibliographicItemId bibId = new BibliographicItemId();
		bibId.setBibliographicItemIdentifier(barcode);
		bibId.setBibliographicItemIdentifierCode(bibItemIdCode);
		itemIds.add(bibId);
		bibDesc.setBibliographicItemIds(itemIds);
		iof.setBibliographicDescription(bibDesc);
	}

	private int getBibIdIndex(List<BibliographicId> bibIds, String bibId) {

		for (int i = 0; i < bibIds.size(); i++) {
			BibliographicId bibIdFromList = bibIds.get(i);
			if (bibIdFromList.getBibliographicItemId() != null) {
				if (bibIds.get(i).getBibliographicItemId().getBibliographicItemIdentifier().equalsIgnoreCase(bibId)) {
					return i;
				}
			} else if (bibIdFromList.getBibliographicRecordId() != null) {
				if (bibIds.get(i).getBibliographicRecordId().getBibliographicRecordIdentifier().equalsIgnoreCase(bibId)) {
					return i;
				}
			}
		}

		return -1;
	}
}
