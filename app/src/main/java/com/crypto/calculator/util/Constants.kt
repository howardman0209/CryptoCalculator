package com.crypto.calculator.util

const val REQUEST_CODE_ALL_PERMISSION = 1001
const val REQUEST_CODE_CAMERA = 1002
const val REQUEST_CODE_BLUETOOTH = 1003
const val REQUEST_CODE_LOCATION = 1004
const val REQUEST_CODE_FILE = 1005
const val REQUEST_CODE_READ_FILE = 1006
const val REQUEST_CODE_NOTIFICATION = 1007
const val REQUEST_CODE_PHONE_STATE = 1008
const val REQUEST_CODE_QR_CODE_ACTIVITY = 0x10

//date time display
const val MONTH_DATE_DISPLAY_PATTERN = "MMM dd"
const val MONTH_YEAR_DISPLAY_PATTERN = "MMM yyyy"
const val MONTH_DISPLAY_PATTERN = "MMM"
const val YEAR_DISPLAY_PATTERN = "yyyy"
const val DATE_DISPLAY_PATTERN = "MMM dd, yyyy"
const val DATE_TIME_DISPLAY_PATTERN_SHORT = "MMM dd, HH:mm"
const val DATE_TIME_DISPLAY_PATTERN_SO_SHORT = "HH:mm:ss"
const val DATE_TIME_DISPLAY_PATTERN_FULL = "MMM dd yyyy, HH:mm"
const val DATE_TIME_PATTERN_UTC = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
const val DATE_TIME_DISPLAY_PATTERN_LONG = "yyyy-MM-dd HH:mm:ss"
const val DATE_TIME_PATTERN_UTC_DEFAULT = "yyyy-MM-dd'T'HH:mm:ssz"

const val DATE_TIME_PATTERN_EMV_9A = "YYMMdd"
const val DATE_TIME_PATTERN_EMV_9F21 = "HHmmSS"

//APDU command response code
const val APDU_RESPONSE_CODE_OK = "9000"
//const val APDU_RESPONSE_CODE_WARNING_A = "62"
//const val APDU_RESPONSE_CODE_WARNING_B = "63"
//const val APDU_RESPONSE_CODE_ERROR_A = "69"
//const val APDU_RESPONSE_CODE_ERROR_B = "6A"

const val APDU_COMMAND_1PAY_SYS_DDF01 = "00A404000E315041592E5359532E444446303100"
const val APDU_COMMAND_2PAY_SYS_DDF01 = "00A404000E325041592E5359532E444446303100"

const val APDU_COMMAND_GPO_WITHOUT_PDOL = "80A8000002830000"
const val APDU_COMMAND_GET_CHALLENGE = "0084000000"

//time zone
const val timeZone_HK = "GMT+8"

//log tag
const val LIFECYCLE = "LIFECYCLE"
const val TAG = "DEBUG"

//shared preference file
const val localDataPrefFileName = "localDataPref"
const val localPrefFileName = "localPref"
const val localeLanguagePrefKey = "localeLanguage"
const val localeCountryPrefKey = "localeCountry"

//prefKey
const val prefLastUsedTool= "prefLastUsedTool"
const val prefCardPreference = "CardPreference"
const val prefCardProfile = "CardProfile"
const val prefEmvConfig = "prefEmvConfig"
const val prefCapkData = "prefCapkData"

const val CVM_SIGNATURE_BINARY_CODE = "011110"
const val CVM_NO_CVM_BINARY_CODE = "011111"

//assets path
const val assetsPathEmvConfig = "emv/config/emvConfig.cfg"
const val assetsPathTestCapk = "emv/config/testKey.capk"
const val assetsPathLiveCapk = "emv/config/liveKey.capk"
const val assetsPathAresLicense = "ares/slm-Spectratech-license_file.json"
const val assetsPathTerminalEmvConfig = "emv/terminal/emvConfig.json"
const val assetsPathCardVisa = "emv/card/visa"
const val assetsPathCardMaster = "emv/card/mastercard"
const val assetsPathCardUnionPay = "emv/card/unionpay.json"
const val assetsPathCardJcb= "emv/card/jcb.json"
const val assetsPathCardDiscover= "emv/card/discover.json"
const val assetsPathCardAmex= "emv/card/amex.json"