package org.extensiblecatalog.ncip.v2.aleph.AlephXServices.xservice;

import org.extensiblecatalog.ncip.v2.aleph.util.AlephConstants;
import org.extensiblecatalog.ncip.v2.aleph.util.AlephException;

import java.io.Serializable;
import java.util.List;


/**
 * This is a factory class that is used to create an X-Service that can
 * be used to execute a remote X-Service call against Aleph.
 * 
 * @author Rick Johnson (NDU)
 */
public class XServiceFactory implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4113712769506551983L;

	/**
	 * createBorAuthXService
	 * 
	 * Create an X-Service that will make a bor-auth X-Service call
	 * 
	 * @param library
	 * @param sub_library
	 * @param patron_id
	 * @param password
	 * @return XService object initialized with values passed in
	 */
	public static XService createBorAuthXService(String library, String sub_library, 
			String patron_id, String password){
		XService xService = new XService(AlephConstants.XSERVICE_BOR_AUTH);
		if (library!=null) xService.addParameter(AlephConstants.PARAM_LIBRARY, library);
		if (patron_id!=null) xService.addParameter(AlephConstants.PARAM_PATRON_ID,patron_id);
		if (password!=null) xService.addParameter(AlephConstants.PARAM_PASSWORD,password);
		if (sub_library!=null) xService.addParameter(AlephConstants.PARAM_SUB_LIBRARY,sub_library);
		
		return xService;
	}
	
	/**
	 * Create an XService object that will query the X-Server for bor-info (patron
	 * info). The library and patron_id values are mandatory.  If password is null,
	 * it will be ignored.  If getCashData is false, all other cash parameters will
	 * be ignored.  If cashFilter is not null, cashBalanceOnly and openFinesOnly will
	 * be ignored.  If getHolds is false, partialHoldData will be ignored. 
	 * If getLoans is false, partialLoanData will be ignored.
	 * If getCashData, getHolds, and getLoans are true the default value
	 * is to get all data for a patron.
	 * 
	 * @param library  			Library for a user
	 * @param patron_id 		patron id within the library
	 * @param password 			password used for verification
	 * 
	 * @param cashDataType		set type of cash data returned, possible values are:
	 * 							- Y (XService.CASH_ALL_DATA) default if cashDataType is null or unrecognized, return full cash data
	 * 							- N (XService.CASH_NO_DATA) do not return any cash data
	 * 							- B (XService.CASH_BALANCE_ONLY) return cash balances only
	 * 							- O	(XService.CASH_OPEN_TRANSACTIONS_ONLY) return open cash transactions only
	 * 							- F (XService.CASH_FILTER) use a cash filter string, cashDataType will revert to Y if cashFilter is null
	 * @param cashFilter		Set a cash filter used for cash data retrieval
	 * @param holdDataType		possible values are:
	 * 							- Y (XService.HOLDS_ALL_DATA) default if holdDataType is null or unrecognized, return full hold data
	 * 							- N (XService.HOLDS_NO_DATA) do not return any hold data
	 * 							- P (XService.HOLDS_PARTIAL_DATA) return partial hold data
	 * @param loanDataType		possible values are:
	 * 							- Y (XService.LOANS_ALL_DATA) default if loanDataType is null or unrecognized, return full loan data
	 * 							- N (XService.LOANS_NO_DATA) do not return any loan data
	 * 							- P (XService.LOANS_PARTIAL_DATA) return partial loan data
	 * 
	 * @return XService object configured with the supplied values
	 */
	public static XService createBorInfoXService(String library, String patron_id, String password,
			String cashDataType, String cashFilter, String holdDataType, String loanDataType){
		XService xService = new XService(AlephConstants.XSERVICE_BOR_INFO);
		if (library!=null) xService.addParameter(AlephConstants.PARAM_LIBRARY, library);
		if (patron_id!=null) xService.addParameter(AlephConstants.PARAM_PATRON_ID,patron_id);
		if (password!=null) xService.addParameter(AlephConstants.PARAM_PASSWORD,password);
		
		String cash = AlephConstants.USER_CASH_ALL_DATA;
		if (cashDataType!=null){
			if (cashDataType.equalsIgnoreCase(AlephConstants.USER_CASH_NO_DATA)){
				cash = AlephConstants.USER_CASH_NO_DATA;
			} else if (cashDataType.equalsIgnoreCase(AlephConstants.USER_CASH_FILTER)&&cashFilter!=null){
				cash = AlephConstants.USER_CASH_FILTER;
			} else if (cashDataType.equalsIgnoreCase(AlephConstants.USER_CASH_BALANCE_ONLY)){
				cash = AlephConstants.USER_CASH_BALANCE_ONLY;
			} else if (cashDataType.equalsIgnoreCase(AlephConstants.USER_CASH_OPEN_TRANSACTIONS_ONLY)){
				cash = AlephConstants.USER_CASH_OPEN_TRANSACTIONS_ONLY;
			}
		}
		xService.addParameter(AlephConstants.PARAM_CASH, cash);
		if (cash.equals(AlephConstants.USER_CASH_FILTER)){
			xService.addParameter(AlephConstants.PARAM_FILTER_CASH, cashFilter);
		}
		
		String holds = AlephConstants.USER_HOLDS_ALL_DATA;
		if (holdDataType!=null){
			if (holdDataType.equalsIgnoreCase(AlephConstants.USER_HOLDS_NO_DATA)){
				holds = AlephConstants.USER_HOLDS_NO_DATA;
			} else if (holdDataType.equalsIgnoreCase(AlephConstants.USER_HOLDS_PARTIAL_DATA)){
				holds = AlephConstants.USER_HOLDS_PARTIAL_DATA;
			}
		}
		xService.addParameter(AlephConstants.PARAM_HOLD, holds);
		
		String loans = AlephConstants.USER_LOANS_ALL_DATA;
		if (loanDataType!=null){
			if (loanDataType.equalsIgnoreCase(AlephConstants.USER_LOANS_NO_DATA)){
				loans = AlephConstants.USER_LOANS_NO_DATA;
			} else if (loanDataType.equalsIgnoreCase(AlephConstants.USER_LOANS_PARTIAL_DATA)){
				loans = AlephConstants.USER_LOANS_PARTIAL_DATA;
			}
		}
		xService.addParameter(AlephConstants.PARAM_LOANS, loans);
		
		return xService;
	}
	
	/**
	 * 
	 * createCircStatusXService
	 * 
	 * Create an X-Service that will make a circ-status X-Service call
	 * 
	 * @param library
	 * @param system_number
	 * @return XService object initialized with values passed in
	 */
	public static XService createCircStatusXService(String library, String system_number){
		XService xService = new XService(AlephConstants.XSERVICE_CIRC_STATUS);
		xService.addParameter(AlephConstants.PARAM_LIBRARY, library);
		xService.addParameter(AlephConstants.PARAM_SYSTEM_NUMBER,system_number);
		return xService;
	}
	
	/**
	 * Create Item Data XService
	 * 
	 * @param base
	 * @param document_number
	 * @return XService object
	 */
	public static XService createItemDataXService(String base, String document_number){
		XService xService = new XService(AlephConstants.XSERVICE_ITEM_DATA);
		xService.addParameter(AlephConstants.PARAM_DOC_NUMBER,document_number);
		xService.addParameter(AlephConstants.PARAM_BASE, base);
		return xService;
	}
	
	/**
	 * Create Read Item XService based on adm library and barcode
	 * @param library
	 * @param barcode
	 * @return XService object
	 */
	public static XService createReadItemXService(String library, String barcode){
		XService xService = new XService(AlephConstants.XSERVICE_READ_ITEM);
		xService.addParameter(AlephConstants.PARAM_LIBRARY, library);
		xService.addParameter(AlephConstants.PARAM_BARCODE, barcode);
		return xService;
	}
	
	/**
	 * Create Read Item XService based on adm library, doc number, and item sequence
	 * 
	 * @param library
	 * @param document_number
	 * @param item_sequence
	 * @return XService object
	 */
	public static XService createReadItemXService(String library, String document_number, String item_sequence){
		XService xService = new XService(AlephConstants.XSERVICE_READ_ITEM);
		xService.addParameter(AlephConstants.PARAM_LIBRARY, library);
		xService.addParameter(AlephConstants.PARAM_DOC_NUMBER, document_number);
		xService.addParameter(AlephConstants.PARAM_SEQ_NUMBER, item_sequence);
		return xService;
	}
	
	/**
	 * Create Find Document XService
	 * 
	 * @param base
	 * @param document_number
	 * @return XService object
	 */
	public static XService createFindDocXService(String base, String document_number){
		XService xService = new XService(AlephConstants.XSERVICE_FIND_DOC);
		xService.addParameter(AlephConstants.PARAM_DOC_NUMBER,document_number);
		xService.addParameter(AlephConstants.PARAM_BASE, base);
		return xService;
	}
	
	/**
	 * Create Get Holding XService
	 * 
	 * @param base
	 * @param document_number
	 * @return XService object
	 */
	public static XService createGetHoldingXService(String base, String document_number){
		XService xService = new XService(AlephConstants.XSERVICE_GET_HOLDING);
		xService.addParameter(AlephConstants.PARAM_DOC_NUMBER,document_number);
		xService.addParameter(AlephConstants.PARAM_BASE, base);
		return xService;
	}
	
	/**
	 * Create an XService that can initiate a hold request on an item
	 * 
	 * @param library
	 * @param doc_number  This is equivalent to the adm id
	 * @param seq_number  This is the sequence number
	 * @param bor_id   The borrower id of the person requesting the hold
	 * @return XService object
	 */
	public static XService createHoldRequestXService(String library, String doc_number, String seq_number, String bor_id){
		XService xService = new XService(AlephConstants.XSERVICE_HOLD_REQUEST);
		xService.addParameter(AlephConstants.PARAM_LIBRARY,library);
		xService.addParameter(AlephConstants.PARAM_DOC_NUMBER, doc_number);
		xService.addParameter(AlephConstants.PARAM_SEQ_NUMBER, seq_number);
		xService.addParameter(AlephConstants.PARAM_PATRON_ID, bor_id);
		return xService;
	}
	
	/**
	 * Create an XService that can initiate a hold request on an item
	 * 
	 * @param library
	 * @param barcode  This does not need to be set if rec_key is set
	 * @param bor_id   The borrower id of the person requesting the hold
	 * @return XService object
	 */
	public static XService createHoldRequestXService(String library, String barcode, String bor_id){
		XService xService = new XService(AlephConstants.XSERVICE_HOLD_REQUEST);
		xService.addParameter(AlephConstants.PARAM_LIBRARY,library);
		xService.addParameter(AlephConstants.PARAM_BARCODE, barcode);
		xService.addParameter(AlephConstants.PARAM_PATRON_ID, bor_id);
		return xService;
	}
	
	/**
	 * Create an XService object that can cancel a hold request
	 * 
	 * @param library
	 * @param item_id  This equates to adm id plus seq number
	 * @param hold_id
	 * @return XService object
	 */
	public static XService createCancelHoldRequestXService(String library, String item_id, String hold_id){
		XService xService = new XService(AlephConstants.XSERVICE_CANCEL_HOLD_REQUEST);
		xService.addParameter(AlephConstants.PARAM_LIBRARY, library);
		xService.addParameter(AlephConstants.PARAM_REC_KEY,item_id+hold_id);
		return xService;
	}
	
	/**
	 * Create an XService object that can cancel a hold request
	 * 
	 * @param library
	 * @param doc_number
	 * @param seq_number
	 * @param hold_id
	 * @return XService object
	 */
	public static XService createCancelHoldRequestXService(String library, String doc_number, String seq_number, String hold_id){
		XService xService = new XService(AlephConstants.XSERVICE_CANCEL_HOLD_REQUEST);
		xService.addParameter(AlephConstants.PARAM_LIBRARY, library);
		xService.addParameter(AlephConstants.PARAM_DOC_NUMBER,doc_number);
		xService.addParameter(AlephConstants.PARAM_SEQ_NUMBER,seq_number);
		xService.addParameter(AlephConstants.PARAM_CANCEL_SEQUENCE,hold_id);
		return xService;
	}
	
	/**
	 * Create an XService that will renew an item
	 * 
	 * @param library
	 * @param doc_number  This is equivalent to the adm id
	 * @param seq_number  This is the sequence number
	 * @param bor_id   The borrower id of the person requesting the renewal
	 * @return XService object
	 */
	public static XService createRenewXService(String library, String doc_number, String seq_number, String bor_id){
		XService xService = new XService(AlephConstants.XSERVICE_RENEW);
		xService.addParameter(AlephConstants.PARAM_LIBRARY,library);
		xService.addParameter(AlephConstants.PARAM_DOC_NUMBER, doc_number);
		xService.addParameter(AlephConstants.PARAM_SEQ_NUMBER, seq_number);
		xService.addParameter(AlephConstants.PARAM_PATRON_ID, bor_id);
		return xService;
	}
	
	/**
	 * Create an XService that will renew an item
	 * 
	 * @param library
	 * @param barcode  This does not need to be set if rec_key is set
	 * @param bor_id   The borrower id of the person renewing the item
	 * @return XService object
	 */
	public static XService createRenewXService(String library, String barcode, String bor_id){
		XService xService = new XService(AlephConstants.XSERVICE_RENEW);
		xService.addParameter(AlephConstants.PARAM_LIBRARY,library);
		xService.addParameter(AlephConstants.PARAM_BARCODE, barcode);
		xService.addParameter(AlephConstants.PARAM_PATRON_ID, bor_id);
		return xService;
	}
	
	/**
	 * Create xservice that takes a list of bibids and returns availability. If bibIds is 
	 * greater than 10 it will throw an exception.
	 * 
	 * @param bib_library
	 * @param bibIds
	 * @return
	 * @throws AlephException 
	 */
	public static XService createPublishAvailabilityXService(String bib_library, List<String> bibIds) throws AlephException{
		XService xService = new XService(AlephConstants.XSERVICE_PUBLISH_AVAILABILITY);
		xService.addParameter(AlephConstants.PARAM_LIBRARY, bib_library);
		//create string separated by commas
		String ids = "";
		boolean first = true;
		if (bibIds!=null){
			if (bibIds.size()>10){
				throw new AlephException(AlephConstants.ERROR_AVAILABILITY_LIST_TOO_LONG);
			}
			for (String bibId : bibIds){
				if (bibId!=null){
					if (!first){
						ids += ",";
					}
					ids += bibId;
					first = false;
				}
			}
			xService.addParameter(AlephConstants.PARAM_DOC_NUM, ids);
		}
		return xService;
	}
}
