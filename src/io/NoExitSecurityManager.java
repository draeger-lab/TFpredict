/*
    TFpredict performs the identification and structural characterization
    of transcription factors.
    Copyright (C) 2012 ZBIT, University of Tübingen, Florian Topf and Johannes Eichner

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io;

import java.security.Permission;

/**
 * 
 * @author Florian Topf
 * @version $Rev$
 * @since 1.0
 */
public class NoExitSecurityManager extends SecurityManager {
	
	protected static class ExitException extends SecurityException 
    {
    	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final int status;
    	public ExitException(int status) 
    	{
    		super("There is no escape!");
    		this.status = status;
    	}
    }
	
	@Override
	public void checkPermission(Permission perm) 
	{
		// allow anything.
	}
	@Override
	public void checkPermission(Permission perm, Object context) 
	{
		// allow anything.
	}
	@Override
	public void checkExit(int status) 
	{
		super.checkExit(status);
		throw new ExitException(status);
	}
	

}
