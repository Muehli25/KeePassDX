/*

KeePass for J2ME

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This file was derived from 

Java clone of KeePass - A KeePass file viewer for Java
Copyright 2006 Bill Zwicky <billzwicky@users.sourceforge.net>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.keepassdroid.database;

// Java
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.keepassdroid.database.exception.InvalidKeyFileException;

/**
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwDatabaseV3 extends PwDatabase {
	// TODO: delete ME
	public byte[] postHeader;

	// Constants
	// private static final int PWM_SESSION_KEY_SIZE = 12;

	// Special entry for settings
	public PwEntry metaInfo;

	// all entries
	public List<PwEntry> entries = new ArrayList<PwEntry>();
	// all groups
	private List<PwGroup> groups = new ArrayList<PwGroup>();
	// Algorithm used to encrypt the database
	public PwEncryptionAlgorithm algorithm;
	public int numKeyEncRounds;

	// Debugging entries
	public PwDbHeaderV3 dbHeader;

	@Override
	public PwEncryptionAlgorithm getEncAlgorithm() {
		return algorithm;
	}

	public int getNumKeyEncRecords() {
		return numKeyEncRounds;
	}

	@Override
	public List<PwGroup> getGroups() {
		return groups;
	}

	@Override
	public List<PwEntry> getEntries() {
		return entries;
	}

	public void setGroups(List<PwGroup> grp) {
		groups = grp;
	}

	@Override
	public List<PwGroup> getGrpRoots() {
		int target = 0;
		List<PwGroup> kids = new ArrayList<PwGroup>();
		for (int i = 0; i < groups.size(); i++) {
			PwGroupV3 grp = (PwGroupV3) groups.get(i);
			if (grp.level == target)
				kids.add(grp);
		}
		return kids;
	}

	public int getRootGroupId() {
		for (int i = 0; i < groups.size(); i++) {
			PwGroupV3 grp = (PwGroupV3) groups.get(i);
			if (grp.level == 0) {
				return grp.groupId;
			}
		}

		return -1;
	}

	public List<PwGroup> getGrpChildren(PwGroupV3 parent) {
		int idx = groups.indexOf(parent);
		int target = parent.level + 1;
		List<PwGroup> kids = new ArrayList<PwGroup>();
		while (++idx < groups.size()) {
			PwGroupV3 grp = (PwGroupV3) groups.get(idx);
			if (grp.level < target)
				break;
			else if (grp.level == target)
				kids.add(grp);
		}
		return kids;
	}

	public List<PwEntry> getEntries(PwGroupV3 parent) {
		List<PwEntry> kids = new ArrayList<PwEntry>();
		/*
		 * for( Iterator i = entries.iterator(); i.hasNext(); ) { PwEntryV3 ent
		 * = (PwEntryV3)i.next(); if( ent.groupId == parent.groupId ) kids.add(
		 * ent ); }
		 */
		for (int i = 0; i < entries.size(); i++) {
			PwEntryV3 ent = (PwEntryV3) entries.get(i);
			if (ent.groupId == parent.groupId)
				kids.add(ent);
		}
		return kids;
	}

	public String toString() {
		return name;
	}

	public void addGroup(PwGroupV3 group) {
		groups.add(group);
	}

	public void addEntry(PwEntry entry) {
		entries.add(entry);
	}

	public void constructTree(PwGroupV3 currentGroup) {
		// I'm in root
		if (currentGroup == null) {
			PwGroupV3 root = new PwGroupV3();
			rootGroup = root;

			List<PwGroup> rootChildGroups = getGrpRoots();
			root.setGroups(rootChildGroups);
			root.childEntries = new ArrayList<PwEntry>();
			root.level = -1;
			for (int i = 0; i < rootChildGroups.size(); i++) {
				PwGroupV3 grp = (PwGroupV3) rootChildGroups.get(i);
				grp.parent = root;
				constructTree(grp);
			}
			return;
		}

		// I'm in non-root
		// get child groups
		currentGroup.setGroups(getGrpChildren(currentGroup));
		currentGroup.childEntries = getEntries(currentGroup);

		// set parent in child entries
		for (int i = 0; i < currentGroup.childEntries.size(); i++) {
			PwEntryV3 entry = (PwEntryV3) currentGroup.childEntries.get(i);
			entry.parent = currentGroup;
		}
		// recursively construct child groups
		for (int i = 0; i < currentGroup.childGroups.size(); i++) {
			PwGroupV3 grp = (PwGroupV3) currentGroup.childGroups.get(i);
			grp.parent = currentGroup;
			constructTree((PwGroupV3) currentGroup.childGroups.get(i));
		}
		return;
	}

	public PwGroupV3 newGroup(String name, PwGroupV3 parent) {
		// Initialize group
		PwGroupV3 group = new PwGroupV3();

		group.parent = parent;
		group.groupId = newGroupId();
		group.imageId = 0;
		group.name = name;

		Date now = Calendar.getInstance().getTime();
		group.tCreation = new PwDate(now);
		group.tLastAccess = new PwDate(now);
		group.tLastMod = new PwDate(now);
		group.tExpire = new PwDate(PwGroupV3.NEVER_EXPIRE);

		group.level = parent.level + 1;

		group.childEntries = new ArrayList<PwEntry>();
		group.setGroups(new ArrayList<PwGroup>());

		// Add group PwDatabaseV3 and Parent
		parent.childGroups.add(group);
		groups.add(group);

		return group;
	}

	public void removeGroup(PwGroupV3 group) {
		group.parent.childGroups.remove(group);
		groups.remove(group);
	}

	/**
	 * Generates an unused random group id
	 * 
	 * @return new group id
	 */
	private int newGroupId() {
		boolean foundUnusedId = false;
		int newId = 0;

		Random random = new Random();

		while (!foundUnusedId) {
			newId = random.nextInt();

			if (!isGroupIdUsed(newId)) {
				foundUnusedId = true;
			}
		}

		return newId;
	}

	/**
	 * Determine if an id number is already in use
	 * 
	 * @param id
	 *            ID number to check for
	 * @return True if the ID is used, false otherwise
	 */
	private boolean isGroupIdUsed(int id) {
		for (int i = 0; i < groups.size(); i++) {
			PwGroupV3 group = (PwGroupV3) groups.get(i);
			if (group.groupId == id) {
				return true;
			}
		}

		return false;
	}

	public byte[] getMasterKey(String key, String keyFileName)
			throws InvalidKeyFileException, IOException {
		assert (key != null && keyFileName != null);

		if (key.length() > 0 && keyFileName.length() > 0) {
			return getCompositeKey(key, keyFileName);
		} else if (key.length() > 0) {
			return getPasswordKey(key);
		} else if (keyFileName.length() > 0) {
			return getFileKey(keyFileName);
		} else {
			throw new IllegalArgumentException("Key cannot be empty.");
		}

	}

	public byte[] getPasswordKey(String key) throws IOException {
		return getPasswordKey(key, "ISO-8859-1");
	}

	@Override
	public long getNumRounds() {
		return numKeyEncRounds;
	}

	@Override
	public void setNumRounds(long rounds) throws NumberFormatException {
		if (rounds > Integer.MAX_VALUE || rounds < Integer.MIN_VALUE) {
			throw new NumberFormatException();
		}

		numKeyEncRounds = (int) rounds;
	}

	@Override
	public boolean appSettingsEnabled() {
		return true;
	}

}
