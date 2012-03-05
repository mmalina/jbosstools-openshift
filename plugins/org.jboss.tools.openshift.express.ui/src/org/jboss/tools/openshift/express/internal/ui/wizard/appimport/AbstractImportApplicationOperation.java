/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.appimport;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.core.GitIgnore;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
abstract class AbstractImportApplicationOperation implements IImportApplicationStrategy {

	private String projectName;
	private IApplication application;
	private String remoteName;
	protected List<IResource> modifiedResources;

	public AbstractImportApplicationOperation(String projectName, IApplication application, String remoteName) {
		this.projectName = projectName;
		this.application = application;
		this.remoteName = remoteName;
		this.modifiedResources = new ArrayList<IResource>();
	}

	/**
	 * Clones the repository of the selected OpenShift application to the user
	 * provided path.
	 * 
	 * @param application
	 *            the application to clone
	 * @param remoteName
	 *            the name of the remote repo to clone
	 * @param destination
	 *            the destination to clone to
	 * @param addToRepoView
	 *            if true, the clone repo will get added to the (egit)
	 *            repositories view
	 * @param monitor
	 *            the monitor to report progress to
	 * 
	 * @return the location of the cloned repository
	 * @throws OpenShiftException
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 * 
	 * @see AbstractImportApplicationOperation#getApplication()
	 * @see #getRepositoryPath()
	 */
	protected File cloneRepository(IApplication application, String remoteName, File destination,
			boolean addToRepoView, IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, URISyntaxException {
		monitor.subTask(NLS.bind("Cloning repository for application {0}...", application.getName()));
		EGitUIUtils.ensureEgitUIIsStarted();
		if (addToRepoView) {
			EGitUtils.cloneRepository(
					application.getGitUri(), remoteName, destination, EGitUIUtils.ADD_TO_REPOVIEW_TASK, monitor);
		} else {
			EGitUtils.cloneRepository(
					application.getGitUri(), remoteName, destination, monitor);
		}
		return destination;
	}

	/**
	 * Adds the given remote repo (at the given git uri) with the given name to
	 * the given repository. The remote is not added if the remoteName to use is
	 * "origin".
	 * 
	 * @param remoteName
	 *            the name to store the remote repo with
	 * @param gitUri
	 *            the git uri at which the remote repo is reachable
	 * @param repository
	 *            the local repo to add the remote to
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	protected void addRemoteRepo(String remoteName, String gitUri, Repository repository) throws MalformedURLException,
			URISyntaxException, IOException {
		if (remoteName != Constants.DEFAULT_REMOTE_NAME) {
			EGitUtils.addRemoteTo(remoteName, gitUri, repository);
		}
	}

	protected String getProjectName() {
		return projectName;
	}

	/**
	 * Returns the workspace project with the given name if it exists. Returns
	 * <code>null</code> otherwise.
	 * 
	 * @param name
	 *            the project name
	 * @return the project with the given name
	 */
	private IProject getProject(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		Assert.isTrue(project != null && project.exists(),
				NLS.bind("Could not find project {0} in your workspace.", name));
		return project;
	}

	protected IProject getProject() {
		return getProject(getProjectName());
	}

	protected IApplication getApplication() {
		return application;
	}

	protected String getRemoteName() {
		return remoteName;
	}

	/**
	 * Marks the given resources as modified.
	 * 
	 * @param resources
	 *            the resources that shall be marked as modified
	 * 
	 * @see #addAndCommitModifiedResource(IProject, IProgressMonitor)
	 */
	protected void addToModified(Collection<IResource> resources) {
		if (resources == null) {
			return;
		}
		modifiedResources.addAll(resources);
	}

	/**
	 * 
	 * Marks the given resource as modified.
	 * 
	 * @param resource
	 *            the resource that shall be marked as modified
	 * 
	 * @see #addAndCommitModifiedResource(IProject, IProgressMonitor)
	 */
	protected void addToModified(IResource resource) {
		if (resource == null) {
			return;
		}
		modifiedResources.add(resource);
	}

	/**
	 * Adds and commits all (modified) resources in the given project to the git
	 * repository that it is attached to.
	 * 
	 * @param project
	 *            the project to commit
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws CoreException
	 * @throws OpenShiftException
	 * 
	 * @see #addToModified(Collection<IResource>)
 	 * @see #addToModified(IResource)
 	 * 
	 */
	protected void addAndCommitModifiedResource(IProject project, IProgressMonitor monitor) throws CoreException,
			OpenShiftException {
		Repository repository = EGitUtils.getRepository(project);
		if (repository == null) {
			throw new OpenShiftException("project {0} is not connected to a git repository.", project.getName());
		}
		new AddToIndexOperation(modifiedResources).execute(monitor);
		EGitUtils.commit(project, monitor);
	}

	/**
	 * Adds a predefined set of entries to the gitignore file in (root of) the
	 * given project. If no .gitignore exists yet, a fresh one is created.
	 * 
	 * @param project
	 *            the project to which the .gitignore shall be configured
	 * @return
	 * @throws IOException
	 * @throws CoreException
	 */
	protected IFile setupGitIgnore(IProject project, IProgressMonitor monitor) throws IOException, CoreException {
		GitIgnore gitIgnore = new GitIgnore(project);
		gitIgnore.add("target")
				.add(".settings")
				.add(".project")
				.add(".classpath")
				.add(".factorypath");
		return gitIgnore.write(monitor);
	}

	protected IResource setupOpenShiftMavenProfile(IProject project, IProgressMonitor monitor) throws CoreException {
		if(!OpenShiftMavenProfile.isMavenProject(project)) {
			return null;
		}

		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(project, OpenShiftUIActivator.PLUGIN_ID);
		if (profile.existsInPom()) {
			return null;
		}
		profile.addToPom(project.getName());
		return profile.savePom(monitor);
	}
}
