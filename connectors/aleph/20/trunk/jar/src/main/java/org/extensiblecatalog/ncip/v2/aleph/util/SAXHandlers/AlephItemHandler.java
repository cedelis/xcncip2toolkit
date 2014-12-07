package org.extensiblecatalog.ncip.v2.aleph.util.SAXHandlers;

import java.util.ArrayList;
import java.util.List;

import org.extensiblecatalog.ncip.v2.aleph.item.AlephItem;
import org.extensiblecatalog.ncip.v2.aleph.util.AlephConstants;
import org.extensiblecatalog.ncip.v2.aleph.util.AlephException;
import org.extensiblecatalog.ncip.v2.service.LookupItemInitiationData;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class is constructed to parse XML outputs of Aleph RESTful APIs items
 * 
 * 
 * @author Jiří Kozlovský (MZK)
 * 
 */
public class AlephItemHandler extends DefaultHandler {

	private List<AlephItem> listOfItems;
	private AlephItem currentAlephItem;

	// Required to decide whether is set second call number the one we need
	private String secondCallNoType;

	// Desired services
	private boolean bibDescriptionDesired;
	private boolean circulationStatusDesired;
	private boolean holdQueueLengthDesired;
	private boolean itemDesrciptionDesired;
	private boolean locationDesired;
	private boolean itemRestrictionDesired;

	// Regular item achievements
	private boolean circulationStatusReached = false;
	private boolean holdQueueLengthReached = false;
	private boolean itemDesrciptionReached = false;
	private boolean authorReached = false;
	private boolean isbnReached = false;
	private boolean titleReached = false;
	private boolean publisherReached = false;
	private boolean bibDocNoReached = false;
	private boolean locationReached = false;
	private boolean callNoReached = false;
	private boolean copyNoReached = false;
	private boolean materialReached = false;
	private boolean barcodeReached = false;
	private boolean collectionReached = false;
	private boolean secondCallNoTypeReached = false;
	private boolean secondCallNoReached = false;
	private boolean itemStatusReached = false;

	private boolean localizationDesired = false;

	public List<AlephItem> getListOfItems() {
		return listOfItems;
	}

	public AlephItem getCurrentAlephItem() {
		return currentAlephItem;
	}

	public AlephItemHandler(LookupItemInitiationData initData) throws AlephException {

		bibDescriptionDesired = initData.getBibliographicDescriptionDesired();
		circulationStatusDesired = initData.getCirculationStatusDesired();
		holdQueueLengthDesired = initData.getHoldQueueLengthDesired();
		itemDesrciptionDesired = initData.getItemDescriptionDesired();
		locationDesired = initData.getLocationDesired();
		itemRestrictionDesired = initData.getItemUseRestrictionTypeDesired();

		if (initData.getInitiationHeader() != null && initData.getInitiationHeader().getApplicationProfileType() != null) {
			String localization = initData.getInitiationHeader().getApplicationProfileType().getValue();

			if (localization != null && !localization.isEmpty())
				localizationDesired = true;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		if (qName.equalsIgnoreCase(AlephConstants.ITEM_NODE)) {
			secondCallNoType = null;
			currentAlephItem = new AlephItem();

			String itemLink = attributes.getValue(AlephConstants.HREF_NODE_ATTR);

			if (itemLink != null) {
				// This occurs if it is parsing link with view=full parameter (e.g. using LookupItemSet with BibRecId)
				String[] itemLinkParts = itemLink.split("/");

				currentAlephItem.setItemId(itemLinkParts[5] + AlephConstants.UNIQUE_ITEM_ID_SEPARATOR + itemLinkParts[7]);
			}

			// This is needed in AlephUtil in order to determine if MediumType should be localized or not while parsing item optional fields
			currentAlephItem.setLocalizationDesired(localizationDesired);

			if (listOfItems == null)
				listOfItems = new ArrayList<AlephItem>();

		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_CALL_NUMBER_NODE)) {
			callNoReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_CALL_NUMBER_2_TYPE_NODE)) {
			secondCallNoTypeReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_CALL_NUMBER_2_NODE)) {
			secondCallNoReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_COPY_ID_NODE)) {
			copyNoReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_BARCODE)) {
			barcodeReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_MATERIAL_NODE)) {
			materialReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.STATUS_NODE) && circulationStatusDesired) {
			circulationStatusReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.QUEUE_NODE) && holdQueueLengthDesired) {
			holdQueueLengthReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_DESCRIPTION_NODE) && itemDesrciptionDesired) {
			itemDesrciptionReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_SUB_LIBRARY_NODE) && locationDesired) {
			locationReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_COLLECTION_NODE) && locationDesired) {
			collectionReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_ITEM_STATUS_NODE) && itemRestrictionDesired) {
			itemStatusReached = true;
		} else if (bibDescriptionDesired) {
			if (qName.equalsIgnoreCase(AlephConstants.Z13_AUTHOR_NODE)) {
				authorReached = true;
			} else if (qName.equalsIgnoreCase(AlephConstants.Z13_ISBN_NODE)) {
				isbnReached = true;
			} else if (qName.equalsIgnoreCase(AlephConstants.Z13_TITLE_NODE)) {
				titleReached = true;
			} else if (qName.equalsIgnoreCase(AlephConstants.Z13_PUBLISHER_NODE)) {
				publisherReached = true;
			} else if (qName.equalsIgnoreCase(AlephConstants.Z13_BIB_ID_NODE)) {
				bibDocNoReached = true;
			}
		}

	}

	// END OF PARSING START ELEMENT

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase(AlephConstants.ITEM_NODE)) {
			listOfItems.add(currentAlephItem);
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_CALL_NUMBER_NODE) && callNoReached) {
			callNoReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_CALL_NUMBER_2_TYPE_NODE) && secondCallNoTypeReached) {
			secondCallNoTypeReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_CALL_NUMBER_2_NODE) && secondCallNoReached) {
			secondCallNoReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_COPY_ID_NODE) && copyNoReached) {
			copyNoReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_BARCODE) && barcodeReached) {
			barcodeReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_MATERIAL_NODE) && materialReached) {
			materialReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.STATUS_NODE) && circulationStatusReached) {
			circulationStatusReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.QUEUE_NODE) && holdQueueLengthReached) {
			currentAlephItem.setHoldQueueLength(0);
			holdQueueLengthReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_DESCRIPTION_NODE) && itemDesrciptionReached) {
			itemDesrciptionReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_SUB_LIBRARY_NODE) && locationReached) {
			locationReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_COLLECTION_NODE) && collectionReached) {
			collectionReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_ITEM_STATUS_NODE) && itemStatusReached) {
			itemStatusReached = false;
		} else if (bibDescriptionDesired) {
			if (qName.equalsIgnoreCase(AlephConstants.Z13_AUTHOR_NODE) && authorReached) {
				authorReached = false;
			} else if (qName.equalsIgnoreCase(AlephConstants.Z13_ISBN_NODE) && isbnReached) {
				isbnReached = false;
			} else if (qName.equalsIgnoreCase(AlephConstants.Z13_TITLE_NODE) && titleReached) {
				titleReached = false;
			} else if (qName.equalsIgnoreCase(AlephConstants.Z13_PUBLISHER_NODE) && publisherReached) {
				publisherReached = false;
			} else if (qName.equalsIgnoreCase(AlephConstants.Z13_BIB_ID_NODE) && bibDocNoReached) {
				bibDocNoReached = false;
			}
		}
	}

	// END OF PARSING END ELEMENT

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		if (circulationStatusReached) {
			currentAlephItem.setCirculationStatus(new String(ch, start, length));
			circulationStatusReached = false;
		} else if (holdQueueLengthReached) {
			// Parse this: <queue>1 request(s) of 4 items</queue>
			String parsedHoldQueueLength = (new String(ch, start, length)).split(" ")[0];
			currentAlephItem.setHoldQueueLength(Integer.parseInt(parsedHoldQueueLength));
			holdQueueLengthReached = false;
		} else if (itemDesrciptionReached) {
			currentAlephItem.setDescription(new String(ch, start, length));
			itemDesrciptionReached = false;
		} else if (locationReached) {
			currentAlephItem.setLocation(new String(ch, start, length));
			locationReached = false;
		} else if (callNoReached) {
			currentAlephItem.setCallNumber(new String(ch, start, length));
			callNoReached = false;
		} else if (secondCallNoTypeReached) {
			secondCallNoType = new String(ch, start, length);
			secondCallNoTypeReached = false;
		} else if (secondCallNoReached) {

			// MZK's Aleph ILS has specific settings - when 9 is set as call no. type, then parse it instead of the first.
			// Note that NCIP doesn't allow transfer of two call numbers.

			if (secondCallNoType != null && !secondCallNoType.equalsIgnoreCase("9"))
				currentAlephItem.setCallNumber(new String(ch, start, length));
			else if (secondCallNoType == null)
				currentAlephItem.setCallNumber(new String(ch, start, length));

			secondCallNoReached = false;
		} else if (copyNoReached) {
			currentAlephItem.setCopyNumber(new String(ch, start, length));
			copyNoReached = false;
		} else if (barcodeReached) {
			currentAlephItem.setBarcode(new String(ch, start, length));
			barcodeReached = false;
		} else if (materialReached) {
			currentAlephItem.setMediumType(new String(ch, start, length));
			materialReached = false;
		} else if (collectionReached) {
			currentAlephItem.setCollection(new String(ch, start, length));
			collectionReached = false;
		} else if (itemStatusReached) {
			currentAlephItem.addItemRestriction(new String(ch, start, length));
			itemStatusReached = false;
		} else if (bibDescriptionDesired) {
			if (authorReached) {
				currentAlephItem.setAuthor(new String(ch, start, length));
				authorReached = false;
			} else if (isbnReached) {
				currentAlephItem.setIsbn(new String(ch, start, length));
				isbnReached = false;
			} else if (titleReached) {
				currentAlephItem.setTitle(new String(ch, start, length));
				titleReached = false;
			} else if (publisherReached) {
				currentAlephItem.setPublisher(new String(ch, start, length));
				publisherReached = false;
			} else if (bibDocNoReached) {
				currentAlephItem.setBibId(new String(ch, start, length));
				bibDocNoReached = false;
			}
		}
	}

	// END OF PARSING CHARACTERS INSIDE ELEMENTS
}
