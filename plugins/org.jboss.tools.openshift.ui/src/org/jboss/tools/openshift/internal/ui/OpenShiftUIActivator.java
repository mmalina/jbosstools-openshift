/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui;

import java.net.MalformedURLException;

import org.eclipse.ui.IStartup;
import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.ui.plugin.BaseUIPlugin;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.osgi.framework.BundleContext;

public class OpenShiftUIActivator extends BaseUIPlugin implements IStartup{

	public static final String PLUGIN_ID = "org.jboss.tools.openshift.ui"; //$NON-NLS-1$

	private static OpenShiftUIActivator plugin;
	
	public OpenShiftUIActivator() {
	}
	
	public IPluginLog getLogger(){
		return pluginLogInternal();
	}
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static OpenShiftUIActivator getDefault() {
		return plugin;
	}

	@Override
	//TODO remove early startup once connection logic works
	public void earlyStartup() {
		try{
			ConnectionsRegistrySingleton.getInstance().add(new Connection("http://localhost:8080"));
			ConnectionsRegistrySingleton.getInstance().add(new Connection("https://localhost:8443"));
		}catch(MalformedURLException e){
			throw new RuntimeException(e);
		}
	}
}
