/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package com.granita.contacticloudsync.resource;

import android.util.Log;

import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.granita.contacticloudsync.Constants;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.ValidationWarnings;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImageType;
import ezvcard.parameter.RelatedType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Anniversary;
import ezvcard.property.Birthday;
import ezvcard.property.Categories;
import ezvcard.property.Email;
import ezvcard.property.FormattedName;
import ezvcard.property.Impp;
import ezvcard.property.Logo;
import ezvcard.property.Nickname;
import ezvcard.property.Note;
import ezvcard.property.Organization;
import ezvcard.property.Photo;
import ezvcard.property.ProductId;
import ezvcard.property.RawProperty;
import ezvcard.property.Related;
import ezvcard.property.Revision;
import ezvcard.property.Role;
import ezvcard.property.Sound;
import ezvcard.property.Source;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import ezvcard.property.Uid;
import ezvcard.property.Url;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * Represents a contact. Locally, this is a Contact in the Android
 * device; remote, this is a VCard.
 */
@ToString(callSuper = true)
public class Contact extends Resource {
	private final static String TAG = "contacticloudsync.Contact";

	@Getter @Setter protected VCardVersion vCardVersion = VCardVersion.V3_0;

	public final static String
		PROPERTY_STARRED = "X-contacticloudsync-STARRED",
		PROPERTY_PHONETIC_FIRST_NAME = "X-PHONETIC-FIRST-NAME",
		PROPERTY_PHONETIC_MIDDLE_NAME = "X-PHONETIC-MIDDLE-NAME",
		PROPERTY_PHONETIC_LAST_NAME = "X-PHONETIC-LAST-NAME",
		PROPERTY_SIP = "X-SIP";
		
	public final static EmailType EMAIL_TYPE_MOBILE = EmailType.get("x-mobile");

	public final static TelephoneType
		PHONE_TYPE_CALLBACK = TelephoneType.get("x-callback"),
		PHONE_TYPE_COMPANY_MAIN = TelephoneType.get("x-company_main"),
		PHONE_TYPE_RADIO = TelephoneType.get("x-radio"),
		PHONE_TYPE_ASSISTANT = TelephoneType.get("X-assistant"),
		PHONE_TYPE_MMS = TelephoneType.get("x-mms");
	public final static RelatedType
		RELATED_TYPE_BROTHER = RelatedType.get("brother"),
		RELATED_TYPE_FATHER = RelatedType.get("father"),
		RELATED_TYPE_MANAGER = RelatedType.get("manager"),
		RELATED_TYPE_MOTHER = RelatedType.get("mother"),
		RELATED_TYPE_REFERRED_BY = RelatedType.get("referred-by"),
		RELATED_TYPE_SISTER = RelatedType.get("sister");
	
	@Getter @Setter private String unknownProperties;
	
	@Getter @Setter private boolean starred;
	
	@Getter @Setter private String displayName, nickName;
	@Getter @Setter private String prefix, givenName, middleName, familyName, suffix;
	@Getter @Setter private String phoneticGivenName, phoneticMiddleName, phoneticFamilyName;
	@Getter @Setter private String note;
	@Getter @Setter private Organization organization;
	@Getter @Setter private String jobTitle, jobDescription;
	
	@Getter @Setter private byte[] photo;
	
	@Getter @Setter private Anniversary anniversary;
	@Getter @Setter private Birthday birthDay;

	@Getter private List<Telephone> phoneNumbers = new LinkedList<>();
	@Getter private List<Email> emails = new LinkedList<>();
	@Getter private List<Impp> impps = new LinkedList<>();
	@Getter private List<Address> addresses = new LinkedList<>();
	@Getter private List<String> categories = new LinkedList<>();
	@Getter private List<String> URLs = new LinkedList<>();
	@Getter private List<Related> relations = new LinkedList<>();

	/* instance methods */
	
	Contact(String name, String ETag) {
		super(name, ETag);
	}
	
	Contact(long localID, String resourceName, String eTag) {
		super(localID, resourceName, eTag);
	}
	
	
	@Override
	public void initialize() {
		generateUID();
		name = uid + ".vcf";
	}
	
	protected void generateUID() {
		uid = UUID.randomUUID().toString();
	}

	
	/* VCard methods */

	@SuppressWarnings("LoopStatementThatDoesntLoop")
    @Override
	public void parseEntity(InputStream is, AssetDownloader downloader) throws IOException {
		VCard vcard = Ezvcard.parse(is).first();
		if (vcard == null)
			return;
		
		// now work through all supported properties
		// supported properties are removed from the VCard after parsing
		// so that only unknown properties are left and can be stored separately
		
		// UID
		Uid uid = vcard.getUid();
		if (uid != null) {
			this.uid = uid.getValue();
			vcard.removeProperties(Uid.class);
		} else {
			Log.w(TAG, "Received VCard without UID, generating new one");
			generateUID();
		}

		// X-contacticloudsync-STARRED
		RawProperty starred = vcard.getExtendedProperty(PROPERTY_STARRED);
		if (starred != null && starred.getValue() != null) {
			this.starred = starred.getValue().equals("1");
			vcard.removeExtendedProperty(PROPERTY_STARRED);
		} else
			this.starred = false;
		
		// FN
		FormattedName fn = vcard.getFormattedName();
		if (fn != null) {
			displayName = fn.getValue();
			vcard.removeProperties(FormattedName.class);
		} else
			Log.w(TAG, "Received invalid VCard without FN (formatted name) property");

		// N
		StructuredName n = vcard.getStructuredName();
		if (n != null) {
			prefix = StringUtils.join(n.getPrefixes(), " ");
			givenName = n.getGiven();
			middleName = StringUtils.join(n.getAdditional(), " ");
			familyName = n.getFamily();
			suffix = StringUtils.join(n.getSuffixes(), " ");
			vcard.removeProperties(StructuredName.class);
		}
		
		// phonetic names
		RawProperty
			phoneticFirstName = vcard.getExtendedProperty(PROPERTY_PHONETIC_FIRST_NAME),
			phoneticMiddleName = vcard.getExtendedProperty(PROPERTY_PHONETIC_MIDDLE_NAME),
			phoneticLastName = vcard.getExtendedProperty(PROPERTY_PHONETIC_LAST_NAME);
		if (phoneticFirstName != null) {
			phoneticGivenName = phoneticFirstName.getValue();
			vcard.removeExtendedProperty(PROPERTY_PHONETIC_FIRST_NAME);
		}
		if (phoneticMiddleName != null) {
			this.phoneticMiddleName = phoneticMiddleName.getValue();
			vcard.removeExtendedProperty(PROPERTY_PHONETIC_MIDDLE_NAME);
		}
		if (phoneticLastName != null) {
			phoneticFamilyName = phoneticLastName.getValue();
			vcard.removeExtendedProperty(PROPERTY_PHONETIC_LAST_NAME);
		}
		
		// TEL
		phoneNumbers = vcard.getTelephoneNumbers();
		vcard.removeProperties(Telephone.class);
		
		// EMAIL
		emails = vcard.getEmails();
		vcard.removeProperties(Email.class);
		
		// PHOTO
		for (Photo photo : vcard.getPhotos()) {
			this.photo = photo.getData();
			if (this.photo == null && photo.getUrl() != null)
				try {
					URI uri = new URI(photo.getUrl());
					Log.i(TAG, "Downloading contact photo from " + uri);
					this.photo = downloader.download(uri);
				} catch(Exception e) {
					Log.w(TAG, "Couldn't fetch contact photo", e);
				}
			vcard.removeProperties(Photo.class);
			break;
		}

		// ORG
		organization = vcard.getOrganization();
		vcard.removeProperties(Organization.class);
		// TITLE
		for (Title title : vcard.getTitles()) {
			jobTitle = title.getValue();
			vcard.removeProperties(Title.class);
			break;
		}
		// ROLE
		for (Role role : vcard.getRoles()) {
			this.jobDescription = role.getValue();
			vcard.removeProperties(Role.class);
			break;
		}
	
		// IMPP
		impps = vcard.getImpps();
		vcard.removeProperties(Impp.class);
		
		// NICKNAME
		Nickname nicknames = vcard.getNickname();
		if (nicknames != null) {
			if (nicknames.getValues() != null)
				nickName = StringUtils.join(nicknames.getValues(), ", ");
			vcard.removeProperties(Nickname.class);
		}
		
		// NOTE
		List<String> notes = new LinkedList<String>();
		for (Note note : vcard.getNotes())
			notes.add(note.getValue());
		if (!notes.isEmpty())
			note = StringUtils.join(notes, "\n---\n");
		vcard.removeProperties(Note.class);

		// ADR
		addresses = vcard.getAddresses();
		vcard.removeProperties(Address.class);
		
		// CATEGORY
		Categories categories = vcard.getCategories();
		if (categories != null)
			this.categories = categories.getValues();
		vcard.removeProperties(Categories.class);
		
		// URL
		for (Url url : vcard.getUrls())
			URLs.add(url.getValue());
		vcard.removeProperties(Url.class);

		// BDAY
		birthDay = vcard.getBirthday();
		vcard.removeProperties(Birthday.class);
		// ANNIVERSARY
		anniversary = vcard.getAnniversary();
		vcard.removeProperties(Anniversary.class);

		// RELATED
		for (Related related : vcard.getRelations()) {
			String text = related.getText();
			if (!StringUtils.isNotEmpty(text)) {
				// process only free-form relations with text
				relations.add(related);
				vcard.removeProperty(related);
			}
		}

		// X-SIP
		for (RawProperty sip : vcard.getExtendedProperties(PROPERTY_SIP))
			impps.add(new Impp("sip", sip.getValue()));
		vcard.removeExtendedProperty(PROPERTY_SIP);

		// remove binary properties because of potential OutOfMemory / TransactionTooLarge exceptions
		vcard.removeProperties(Logo.class);
		vcard.removeProperties(Sound.class);
		// remove properties that don't apply anymore
		vcard.removeProperties(ProductId.class);
		vcard.removeProperties(Revision.class);
		vcard.removeProperties(Source.class);
		// store all remaining properties into unknownProperties
		if (!vcard.getProperties().isEmpty() || !vcard.getExtendedProperties().isEmpty())
			try {
				unknownProperties = vcard.write();
			} catch(Exception e) {
				Log.w(TAG, "Couldn't store unknown properties (maybe illegal syntax), dropping them");
			}
	}


	@Override
	public String getMimeType() {
		if (vCardVersion == VCardVersion.V4_0)
			return "text/vcard;version=4.0";
		else
			return "text/vcard";
	}
	
	@Override
	public ByteArrayOutputStream toEntity() throws IOException {
		VCard vcard = null;
		try {
			if (unknownProperties != null)
				vcard = Ezvcard.parse(unknownProperties).first();
		} catch (Exception e) {
			Log.w(TAG, "Couldn't parse original property set, beginning from scratch");
		}
		if (vcard == null)
			vcard = new VCard();

		if (uid != null)
			vcard.setUid(new Uid(uid));
		else
			Log.wtf(TAG, "Generating VCard without UID");
		
		if (starred)
			vcard.setExtendedProperty(PROPERTY_STARRED, "1");
		
		if (displayName != null)
			vcard.setFormattedName(displayName);
		else if (organization != null && organization.getValues() != null && organization.getValues().get(0) != null)
			vcard.setFormattedName(organization.getValues().get(0));
		else
			Log.w(TAG, "No FN (formatted name) available to generate VCard");

		// N
		if (prefix != null || familyName != null || middleName != null || givenName != null || suffix != null) {
			StructuredName n = new StructuredName();
			if (prefix != null)
				for (String p : StringUtils.split(prefix))
					n.addPrefix(p);
			n.setGiven(givenName);
			if (middleName != null)
				for (String middle : StringUtils.split(middleName))
					n.addAdditional(middle);
			n.setFamily(familyName);
			if (suffix != null)
				for (String s : StringUtils.split(suffix))
					n.addSuffix(s);
			vcard.setStructuredName(n);
		}
		
		// phonetic names
		if (phoneticGivenName != null)
			vcard.addExtendedProperty(PROPERTY_PHONETIC_FIRST_NAME, phoneticGivenName);
		if (phoneticMiddleName != null)
			vcard.addExtendedProperty(PROPERTY_PHONETIC_MIDDLE_NAME, phoneticMiddleName);
		if (phoneticFamilyName != null)
			vcard.addExtendedProperty(PROPERTY_PHONETIC_LAST_NAME, phoneticFamilyName);
		
		// TEL
		for (Telephone phoneNumber : phoneNumbers)
			vcard.addTelephoneNumber(phoneNumber);
		
		// EMAIL
		for (Email email : emails)
			vcard.addEmail(email);

		// ORG, TITLE, ROLE
		if (organization != null)
			vcard.setOrganization(organization);
		if (jobTitle != null)
			vcard.addTitle(jobTitle);
		if (jobDescription != null)
			vcard.addRole(jobDescription);
		
		// IMPP
		for (Impp impp : impps)
			vcard.addImpp(impp);

		// NICKNAME
		if (!StringUtils.isBlank(nickName))
			vcard.setNickname(nickName);
		
		// NOTE
		if (!StringUtils.isBlank(note))
			vcard.addNote(note);
		
		// ADR
		for (Address address : addresses)
			vcard.addAddress(address);
		
		// CATEGORY
		if (!categories.isEmpty())
			vcard.setCategories(categories.toArray(new String[0]));
		
		// URL
		for (String url : URLs)
			vcard.addUrl(url);

		// ANNIVERSARY
		if (anniversary != null)
			vcard.setAnniversary(anniversary);
		// BDAY
		if (birthDay != null)
			vcard.setBirthday(birthDay);
		
		// PHOTO
		if (photo != null)
			vcard.addPhoto(new Photo(photo, ImageType.JPEG));

		// REL
		for (Related related : relations)
			vcard.addRelated(related);

		// PRODID, REV
		vcard.setProductId("contacticloudsync/" + Constants.APP_VERSION + " (ez-vcard/" + Ezvcard.VERSION + ")");
		vcard.setRevision(Revision.now());
		
		// validate and print warnings
		ValidationWarnings warnings = vcard.validate(vCardVersion);
		if (!warnings.isEmpty())
			Log.w(TAG, "Created potentially invalid VCard:\n" + warnings);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Ezvcard
			.write(vcard)
			.version(vCardVersion)
			.versionStrict(false)
			.prodId(false)		// we provide our own PRODID
			.go(os);
		return os;
	}
}
