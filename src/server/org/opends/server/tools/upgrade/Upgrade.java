/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2013-2015 ForgeRock AS
 */

package org.opends.server.tools.upgrade;

import static org.opends.messages.ToolMessages.*;
import static org.opends.server.tools.upgrade.FormattedNotificationCallback.*;
import static org.opends.server.tools.upgrade.UpgradeTasks.*;
import static org.opends.server.tools.upgrade.LicenseFile.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.ConfirmationCallback;

import org.opends.messages.Message;
import org.opends.server.core.LockFileManager;
import org.opends.server.tools.ClientException;
import org.opends.server.util.BuildVersion;
import org.opends.server.util.StaticUtils;

/**
 * This class contains the table of upgrade tasks that need performing when
 * upgrading from one version to another.
 */
public final class Upgrade
{
  /**
   * Upgrade's logger.
   */
  private final static Logger LOG = Logger
      .getLogger(UpgradeCli.class.getName());

  /**
   * Upgrade supports version from 2.4.5.
   */
  private final static BuildVersion UPGRADESUPPORTSVERSIONFROM = BuildVersion
      .valueOf("2.4.5.0000");

  /**
   * The success exit code value.
   */
  static final int EXIT_CODE_SUCCESS = 0;

  /**
   * The error exit code value.
   */
  static final int EXIT_CODE_ERROR = 1;

  /**
   * The exit code value that will be used if upgrade requires manual
   * intervention.
   */
  static final int EXIT_CODE_MANUAL_INTERVENTION = 2;

  /**
   * If the upgrade contains some post upgrade tasks to do.
   */
  static boolean hasPostUpgradeTask = false;

  /**
   * Developers should register upgrade tasks below.
   */
  private static final NavigableMap<BuildVersion, List<UpgradeTask>> TASKS =
      new TreeMap<BuildVersion, List<UpgradeTask>>();
  private static final List<UpgradeTask> MANDATORY_TASKS =
      new LinkedList<UpgradeTask>();

  static
  {
    // @formatter:off
    register("2.5.0",
        modifyConfigEntry(INFO_UPGRADE_TASK_6869_SUMMARY.get(),
        "(objectClass=ds-cfg-collation-matching-rule)",
        "add: ds-cfg-collation",
        "ds-cfg-collation: de:1.3.6.1.4.1.42.2.27.9.4.28.1",
        "ds-cfg-collation: de-DE:1.3.6.1.4.1.42.2.27.9.4.28.1",
        "-",
        "delete: ds-cfg-collation",
        "ds-cfg-collation: de:1.3.6.1.4.1.142.2.27.9.4.28.1",
        "ds-cfg-collation: de-DE:1.3.6.1.4.1.142.2.27.9.4.28.1"));

    register("2.5.0",
        modifyConfigEntry(INFO_UPGRADE_TASK_7192_SUMMARY.get(),
        "(objectClass=ds-cfg-password-policy)",
        "add: objectClass",
        "objectClass: ds-cfg-authentication-policy",
        "-",
        "add: ds-cfg-java-class",
        "ds-cfg-java-class: org.opends.server.core.PasswordPolicyFactory"));

    register("2.5.0",
        modifyConfigEntry(INFO_UPGRADE_TASK_7364_SUMMARY.get(),
        "(ds-cfg-java-class=org.opends.server.loggers.TextAuditLogPublisher)",
        "add: objectClass",
        "objectClass: ds-cfg-file-based-audit-log-publisher",
        "-",
        "delete: objectClass",
        "objectClass: ds-cfg-file-based-access-log-publisher"));

    register ("2.5.0",
        renameSnmpSecurityConfig(INFO_UPGRADE_TASK_7466_SUMMARY.get()));

    register("2.5.0",
        newAttributeTypes(INFO_UPGRADE_TASK_7748_1_SUMMARY.get(),
        "00-core.ldif", "etag"),
        addConfigEntry(INFO_UPGRADE_TASK_7748_2_SUMMARY.get(),
        "dn: cn=etag,cn=Virtual Attributes,cn=config",
        "changetype: add",
        "objectClass: top",
        "objectClass: ds-cfg-virtual-attribute",
        "objectClass: ds-cfg-entity-tag-virtual-attribute",
        "cn: etag",
        "ds-cfg-java-class: org.opends.server.extensions."
            + "EntityTagVirtualAttributeProvider",
        "ds-cfg-enabled: true",
        "ds-cfg-attribute-type: etag",
        "ds-cfg-conflict-behavior: real-overrides-virtual",
        "ds-cfg-checksum-algorithm: adler-32",
        "ds-cfg-excluded-attribute: ds-sync-hist"));

    register("2.5.0",
        addConfigEntry(INFO_UPGRADE_TASK_7834_SUMMARY.get(),
        "dn: cn=Password Expiration Time,cn=Virtual Attributes,cn=config",
        "changetype: add",
        "objectClass: top",
        "objectClass: ds-cfg-virtual-attribute",
        "objectClass: ds-cfg-password-expiration-time-virtual-attribute",
        "cn: Password Expiration Time",
        "ds-cfg-java-class: org.opends.server.extensions."
            + "PasswordExpirationTimeVirtualAttributeProvider",
        "ds-cfg-enabled: true",
        "ds-cfg-attribute-type: ds-pwp-password-expiration-time",
        "ds-cfg-conflict-behavior: virtual-overrides-real"));

    register("2.5.0",
        modifyConfigEntry(INFO_UPGRADE_TASK_7979_SUMMARY.get(),
        "(ds-cfg-java-class=org.opends.server.schema.CertificateSyntax)",
        "add: objectClass",
        "objectClass: ds-cfg-certificate-attribute-syntax",
        "-",
        "add: ds-cfg-strict-format",
        "ds-cfg-strict-format: false"));

    register("2.5.0",
        modifyConfigEntry(INFO_UPGRADE_TASK_8124_SUMMARY.get(),
        "(ds-cfg-java-class=org.opends.server.schema.JPEGSyntax)",
        "add: objectClass",
        "objectClass: ds-cfg-jpeg-attribute-syntax",
        "-",
        "add: ds-cfg-strict-format",
        "ds-cfg-strict-format: false"));

    register("2.5.0",
        modifyConfigEntry(INFO_UPGRADE_TASK_8133_SUMMARY.get(),
        "(ds-cfg-java-class=org.opends.server.schema.CountryStringSyntax)",
        "add: objectClass",
        "objectClass: ds-cfg-country-string-attribute-syntax",
        "-",
        "add: ds-cfg-strict-format",
        "ds-cfg-strict-format: false"));

    register("2.5.0",
        modifyConfigEntryOptional(INFO_UPGRADE_TASK_8214_SUMMARY.get(),
        INFO_UPGRADE_TASK_8214_DESCRIPTION.get(),
        "(ds-cfg-java-class=org.opends.server.extensions."
            + "IsMemberOfVirtualAttributeProvider)",
        "add: ds-cfg-filter",
        "ds-cfg-filter: (|(objectClass=person)(objectClass=groupOfNames)"
            + "(objectClass=groupOfUniqueNames)(objectClass=groupOfEntries))",
        "-",
        "delete: ds-cfg-filter",
        "ds-cfg-filter: (objectClass=person)"));

    register("2.5.0",
        modifyConfigEntry(INFO_UPGRADE_TASK_8387_SUMMARY.get(),
        "(objectClass=ds-cfg-dictionary-password-validator)",
        "add: ds-cfg-check-substrings",
        "ds-cfg-check-substrings: false"));

    register("2.5.0",
        modifyConfigEntry(INFO_UPGRADE_TASK_8389_SUMMARY.get(),
        "(objectClass=ds-cfg-attribute-value-password-validator)",
        "add: ds-cfg-check-substrings",
        "ds-cfg-check-substrings: false"));

    register("2.5.0",
        addConfigEntry(INFO_UPGRADE_TASK_8487_SUMMARY.get(),
        "dn: cn=PBKDF2,cn=Password Storage Schemes,cn=config",
        "changetype: add",
        "objectClass: top",
        "objectClass: ds-cfg-password-storage-scheme",
        "objectClass: ds-cfg-pbkdf2-password-storage-scheme",
        "cn: PBKDF2",
        "ds-cfg-java-class: org.opends.server.extensions."
            + "PBKDF2PasswordStorageScheme",
        "ds-cfg-enabled: true"));

    register("2.5.0",
        addConfigFile("http-config.json"),
        addConfigEntry(INFO_UPGRADE_TASK_8613_SUMMARY.get(),
        "dn: cn=HTTP Connection Handler,cn=Connection Handlers,cn=config",
        "changetype: add",
        "objectClass: ds-cfg-http-connection-handler",
        "objectClass: ds-cfg-connection-handler",
        "objectClass: top",
        "ds-cfg-listen-port: 8080",
        "cn: HTTP Connection Handler",
        "ds-cfg-max-blocked-write-time-limit: 2 minutes",
        "ds-cfg-ssl-client-auth-policy: optional",
        "ds-cfg-use-tcp-keep-alive: true",
        "ds-cfg-max-request-size: 5 megabytes",
        "ds-cfg-use-tcp-no-delay: true",
        "ds-cfg-allow-tcp-reuse-address: true",
        "ds-cfg-accept-backlog: 128",
        "ds-cfg-authentication-required: true",
        "ds-cfg-buffer-size: 4096 bytes",
        "ds-cfg-config-file: config/http-config.json",
        "ds-cfg-listen-address: 0.0.0.0",
        "ds-cfg-java-class: " +
          "org.opends.server.protocols.http.HTTPConnectionHandler",
        "ds-cfg-keep-stats: true",
        "ds-cfg-ssl-cert-nickname: server-cert",
        "ds-cfg-use-ssl: false",
        "ds-cfg-enabled: false"));

    register("2.5.0", addConfigEntry(INFO_UPGRADE_TASK_8832_SUMMARY.get(),
        "dn: cn=File-Based HTTP Access Logger,cn=Loggers,cn=config",
        "changetype: add",
        "objectClass: ds-cfg-file-based-http-access-log-publisher",
        "objectClass: top",
        "objectClass: ds-cfg-http-access-log-publisher",
        "objectClass: ds-cfg-log-publisher",
        "cn: File-Based HTTP Access Logger",
        "ds-cfg-java-class: " +
          "org.opends.server.loggers.TextHTTPAccessLogPublisher",
        "ds-cfg-asynchronous: true",
        "ds-cfg-log-file: logs/http-access",
        "ds-cfg-rotation-policy: " +
          "cn=24 Hours Time Limit Rotation Policy," +
          "cn=Log Rotation Policies,cn=config",
        "ds-cfg-rotation-policy: " +
          "cn=Size Limit Rotation Policy,cn=Log Rotation Policies,cn=config",
        "ds-cfg-retention-policy: " +
          "cn=File Count Retention Policy,cn=Log Retention Policies,cn=config",
        "ds-cfg-log-file-permissions: 640",
        "ds-cfg-enabled: false"));

    register("2.5.0",
        newAttributeTypes(INFO_UPGRADE_TASK_8985_1_SUMMARY.get(),
        "00-core.ldif", "emailAddress"),
        modifyConfigEntry(INFO_UPGRADE_TASK_8985_2_SUMMARY.get(),
        "&(ds-cfg-java-class=org.opends.server.extensions." +
        "SubjectAttributeToUserAttributeCertificateMapper)" +
        "(ds-cfg-subject-attribute-mapping=e:mail)",
        "delete:ds-cfg-subject-attribute-mapping",
        "ds-cfg-subject-attribute-mapping: e:mail",
        "-",
        "add:ds-cfg-subject-attribute-mapping",
        "ds-cfg-subject-attribute-mapping: emailAddress:mail"));

    /* See OPENDJ-992 */
    register("2.5.0",
        regressionInVersion("2.5.0.7640",
            rebuildSingleIndex(INFO_UPGRADE_TASK_9013_DESCRIPTION.get(),
                "ds-sync-hist")));

    /** See OPENDJ-1295 */
    register("2.6.1",
        copySchemaFile("03-pwpolicyextension.ldif"));

    /** See OPENDJ-1067 and OPENDJ-1322 */
    register("2.6.1",
            rerunJavaPropertiesTool(INFO_UPGRADE_TASK_9216_SUMMARY.get()));

    /*
     * All upgrades will refresh the server configuration schema and generate
     * a new upgrade folder.
     */
    registerLast(
        copySchemaFile("02-config.ldif"),
        updateConfigUpgradeFolder(),
        postUpgradeRebuildIndexes());

    // @formatter:on
  }

  /**
   * Returns a list containing all the tasks which are required in order to
   * upgrade from {@code fromVersion} to {@code toVersion}.
   *
   * @param fromVersion
   *          The old version.
   * @param toVersion
   *          The new version.
   * @return A list containing all the tasks which are required in order to
   *         upgrade from {@code fromVersion} to {@code toVersion}.
   */
  private static List<UpgradeTask> getUpgradeTasks(
      final BuildVersion fromVersion, final BuildVersion toVersion)
  {
    final List<UpgradeTask> tasks = new LinkedList<UpgradeTask>();
    for (final List<UpgradeTask> subList : TASKS.subMap(fromVersion, false,
        toVersion, true).values())
    {
      tasks.addAll(subList);
    }
    tasks.addAll(MANDATORY_TASKS);
    return tasks;
  }

  /**
   * Upgrades the server from {@code fromVersion} to {@code toVersion} located
   * in the upgrade context.
   *
   * @param context
   *          The context of the upgrade.
   * @throws ClientException
   *           If an error occurred while performing the upgrade.
   */
  public static void upgrade(final UpgradeContext context)
      throws ClientException
  {
    // Checks and validates the version number.
    isVersionCanBeUpdated(context);

    // Server must be offline.
    checkIfServerIsRunning(context);

    context.notify(INFO_UPGRADE_TITLE.get(), TITLE_CALLBACK);
    context.notify(
        INFO_UPGRADE_SUMMARY.get(context.getFromVersion().toString(), context
            .getToVersion().toString()), NOTICE_CALLBACK);
    context.notify(INFO_UPGRADE_GENERAL_SEE_FOR_DETAILS.get(UpgradeUtils
        .getInstallationPath()
        + File.separator + UpgradeLog.UPGRADELOGNAME), NOTICE_CALLBACK);

    // Checks License.
    checkLicence(context);

    /*
     * Get the list of required upgrade tasks.
     */
    final List<UpgradeTask> tasks =
        getUpgradeTasks(context.getFromVersion(), context.getToVersion());
    if (tasks.isEmpty())
    {
      changeBuildInfoVersion(context);
      return;
    }

    /*
     * Verify tasks requirements. E.g. if a task requires mandatory user
     * interaction and the application is non-interactive then, the process may
     * abort immediately.
     */
    for (final UpgradeTask task : tasks)
    {
      task.verify(context);
    }

    /*
     * Let tasks interact with the user in order to obtain user's selection.
     */
    context.notify(INFO_UPGRADE_REQUIREMENTS.get(), TITLE_CALLBACK);
    for (final UpgradeTask task : tasks)
    {
      task.interact(context);
    }

    // Starts upgrade
    final int userResponse =
        context.confirmYN(INFO_UPGRADE_DISPLAY_CONFIRM_START.get(),
            ConfirmationCallback.YES);
    if (userResponse == ConfirmationCallback.NO)
    {
      final Message message = INFO_UPGRADE_ABORTED_BY_USER.get();
      context.notify(message, WARNING);
      throw new ClientException(EXIT_CODE_ERROR, message);
    }

    try
    {
      /*
       * Perform the upgrade tasks.
       */
      context.notify(INFO_UPGRADE_PERFORMING_TASKS.get(), TITLE_CALLBACK);
      for (final UpgradeTask task : tasks)
      {
        task.perform(context);
      }

      if (UpgradeTasks.countErrors == 0)
      {
        /*
         * The end of a successful upgrade is marked up with the build info file
         * update and the license, if present, requires the creation of an
         * approval file.
         */
        changeBuildInfoVersion(context);

        createFileLicenseApproved();
      }
      else
      {
        context.notify(ERR_UPGRADE_FAILS.get(UpgradeTasks.countErrors),
            TITLE_CALLBACK);
      }

      /*
       * Performs the post upgrade tasks.
       */
      if (hasPostUpgradeTask && (UpgradeTasks.countErrors == 0))
      {
        context
            .notify(INFO_UPGRADE_PERFORMING_POST_TASKS.get(), TITLE_CALLBACK);
        performPostUpgradeTasks(context, tasks);
        context.notify(INFO_UPGRADE_POST_TASKS_COMPLETE.get(), TITLE_CALLBACK);
      }
    }
    catch (final ClientException e)
    {
      context.notify(e.getMessageObject(), ERROR_CALLBACK);
      throw e;
    }
    catch (final Exception e)
    {
      final Message message = ERR_UPGRADE_TASKS_FAIL.get(e.getMessage());
      context.notify(message, ERROR_CALLBACK);
      throw new ClientException(EXIT_CODE_ERROR, message);
    }
    finally
    {
      context.notify(INFO_UPGRADE_GENERAL_SEE_FOR_DETAILS.get(UpgradeUtils
          .getInstallationPath()
          + File.separator + UpgradeLog.UPGRADELOGNAME), NOTICE_CALLBACK);
      LOG.log(Level.CONFIG, INFO_UPGRADE_PROCESS_END.get().toString());
    }
  }

  private static void performPostUpgradeTasks(final UpgradeContext context,
      final List<UpgradeTask> tasks) throws ClientException
  {
    boolean isOk = true;
    for (final UpgradeTask task : tasks)
    {
      if (isOk)
      {
        try
        {
          task.postUpgrade(context);
        }
        catch (ClientException e)
        {
          context.notify(e.getMessageObject(), WARNING);
          isOk = false;
        }
      }
      else
      {
        task.postponePostUpgrade(context);
      }
    }
  }

  private static void register(final String versionString,
      final UpgradeTask... tasks)
  {
    final BuildVersion version = BuildVersion.valueOf(versionString);
    List<UpgradeTask> taskList = TASKS.get(version);
    if (taskList == null)
    {
      TASKS.put(version, (taskList = new LinkedList<UpgradeTask>()));
    }
    taskList.addAll(Arrays.asList(tasks));
  }

  private static void registerLast(final UpgradeTask... tasks)
  {
    MANDATORY_TASKS.addAll(Arrays.asList(tasks));
  }

  /**
   * The server must be offline during the upgrade.
   *
   * @throws ClientException
   *           An exception is thrown if the server is currently running.
   */
  private final static void checkIfServerIsRunning(final UpgradeContext context)
      throws ClientException
  {
    final String lockFile = LockFileManager.getServerLockFileName();

    final StringBuilder failureReason = new StringBuilder();
    try
    {
      // Assume that if we cannot acquire the lock file the server is
      // running.
      if (!LockFileManager.acquireExclusiveLock(lockFile, failureReason))
      {
        final Message message = ERR_UPGRADE_REQUIRES_SERVER_OFFLINE.get();
        context.notify(message, NOTICE_CALLBACK);
        throw new ClientException(EXIT_CODE_ERROR, message);
      }
    }
    finally
    {
      LockFileManager.releaseLock(lockFile, failureReason);
    }
  }

  /**
   * Checks if the version can be updated.
   *
   * @param context
   *          The current context which running the upgrade.
   * @throws ClientException
   *           If an exception occurs - stops the process.
   */
  private static void isVersionCanBeUpdated(final UpgradeContext context)
      throws ClientException
  {
    if (context.getFromVersion().equals(context.getToVersion()))
    {
      /*
       * If the server is already up to date then treat it as a successful
       * upgrade so that upgrade is idempotent.
       */
      final Message message =
          ERR_UPGRADE_VERSION_UP_TO_DATE.get(context.getToVersion().toString());
      context.notify(message, NOTICE_CALLBACK);
      throw new ClientException(EXIT_CODE_SUCCESS, message);
    }

    // The upgrade only supports version >= 2.4.5.
    if (context.getFromVersion().compareTo(UPGRADESUPPORTSVERSIONFROM) < 0)
    {
      final Message message =
          INFO_UPGRADE_VERSION_IS_NOT_SUPPORTED.get(UPGRADESUPPORTSVERSIONFROM
              .toString(), UPGRADESUPPORTSVERSIONFROM.toString());
      context.notify(message, NOTICE_CALLBACK);
      throw new ClientException(EXIT_CODE_ERROR, message);
    }
  }

  /**
   * Writes the up to date's version number within the build info file.
   *
   * @param context
   *          The current context which running the upgrade.
   * @throws ClientException
   *           If an exception occurs when displaying the message.
   * @throws IOException
   *           If an exception occurs when trying to write the file.
   */
  private static void changeBuildInfoVersion(final UpgradeContext context)
      throws ClientException
  {
    FileWriter buildInfo = null;
    try
    {
      buildInfo =
          new FileWriter(new File(UpgradeUtils.configDirectory,
              Installation.BUILDINFO_RELATIVE_PATH), false);

      // Write the new version
      buildInfo.write(context.getToVersion().toString());

      context.notify(INFO_UPGRADE_SUCCESSFUL.get(context.getFromVersion()
          .toString(), context.getToVersion().toString()), TITLE_CALLBACK);

    }
    catch (IOException e)
    {
      final Message message = Message.raw(e.getMessage());
      context.notify(message, ERROR_CALLBACK);
      throw new ClientException(EXIT_CODE_ERROR, message);
    }
    finally
    {
      StaticUtils.close(buildInfo);
    }
  }

  private static void checkLicence(final UpgradeContext context)
      throws ClientException
  {
    // Check license
    if (!LicenseFile.isAlreadyApproved())
    {
      if (LicenseFile.exists())
      {
        context.notify(Message.raw("\n" + LicenseFile.getText()));
        context.notify(INFO_LICENSE_DETAILS_CLI_LABEL.get());
        if (!context.isAcceptLicenseMode())
        {
          final int answer;

          // The force cannot answer yes to the license's question,
          // which is not a task even if it requires a user interaction OR
          // -an accept license mode to continue the process.
          if (context.isForceUpgradeMode())
          {
            answer = ConfirmationCallback.NO;
            context.notify(Message.raw(INFO_LICENSE_ACCEPT.get() + " "
                + INFO_PROMPT_NO_COMPLETE_ANSWER.get()));
          }
          else
          {
            answer =
                context.confirmYN(INFO_LICENSE_ACCEPT.get(),
                    ConfirmationCallback.NO);
          }

          if (answer == ConfirmationCallback.NO)
          {
            System.exit(EXIT_CODE_SUCCESS);
          }
          else if (answer == ConfirmationCallback.YES)
          {
            LicenseFile.setApproval(true);
          }
        }
        else
        {
          // We automatically accept the license with this option.
          context.notify(Message.raw(INFO_LICENSE_ACCEPT.get() + " "
              + INFO_PROMPT_YES_COMPLETE_ANSWER.get()));
          LicenseFile.setApproval(true);
        }
      }
    }
  }

  /**
   * Returns {@code true} if the current upgrade contains post upgrade tasks.
   *
   * @return {@code true} if the current upgrade contains post upgrade tasks.
   */
  static boolean hasPostUpgradeTask()
  {
    return hasPostUpgradeTask;
  }

  /**
   * Sets {@code true} if the current upgrade contains post upgrade tasks.
   *
   * @param hasPostUpgradeTask
   *          {@code true} if the current upgrade contains post upgrade tasks.
   */
  static void setHasPostUpgradeTask(boolean hasPostUpgradeTask)
  {
    Upgrade.hasPostUpgradeTask = hasPostUpgradeTask;
  }

  // Prevent instantiation.
  private Upgrade()
  {
    // Nothing to do.
  }
}
