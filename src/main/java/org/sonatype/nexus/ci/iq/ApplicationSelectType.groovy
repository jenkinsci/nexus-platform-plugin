package org.sonatype.nexus.ci.iq

import org.apache.commons.lang.StringUtils
import org.kohsuke.stapler.DataBoundConstructor

class ApplicationSelectType
{
  public static final String MANUAL_TYPE = "manual"

  public static final String LIST_TYPE = "list"

  private String value

  private String applicationId

  // Older versions of Hudson and Jenkins do not post the listAppId and manualAppId in the
  // ApplicationSelectType object.
  public static ApplicationSelectType backfillApplicationSelectType(final ApplicationSelectType applicationSelectType,
                                                                    final String listAppId,
                                                                    final String manualAppId)
  {
    if (applicationSelectType == null) {
      return null
    }
    if (LIST_TYPE == applicationSelectType.value && StringUtils.isBlank(applicationSelectType.applicationId)) {
      applicationSelectType.applicationId = listAppId
    }
    else if (StringUtils.isBlank(applicationSelectType.applicationId)) {
      applicationSelectType.applicationId = manualAppId
    }
    return applicationSelectType
  }

  public static ApplicationSelectType createApplicationSelectTypeIfNull(final ApplicationSelectType applicationSelectType,
                                                                        final String listAppId)
  {
    if (applicationSelectType == null) {
      return new ApplicationSelectType(LIST_TYPE, "", listAppId)
    }
    return applicationSelectType
  }

  @DataBoundConstructor
  public ApplicationSelectType(final String value, final String manualAppId, final String listAppId) {
    this.value = value
    if (LIST_TYPE == value) {
      this.applicationId = listAppId
    }
    else {
      this.applicationId = manualAppId
    }
  }

  public String getValue() {
    return value
  }

  public String getManualAppId() {
    return MANUAL_TYPE == value ? applicationId : ''
  }

  public String getListAppId() {
    return LIST_TYPE == value ? applicationId : ''
  }

  public boolean isManual() {
    return MANUAL_TYPE == value
  }
}
