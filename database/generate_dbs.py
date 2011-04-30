#!/usr/bin/python

import json
import sqlite3
import os

import unicodecsv # https://github.com/jdunck/python-unicodecsv

sqlite_dbpath = 'openbeerdata.db'
bjcp_json = 'bjcp.json'
hops_csv = 'hops.csv'
fermentables_csv = 'fermentables.csv'
srm_csv = 'srm.csv'

version = '20110428'

def setup_sqlite():
	# remove the database if it exists and create a new one

	schema = [
		'''CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US');''',
		
		'''CREATE TABLE db_version (_id INTEGER PRIMARY KEY, version text);''',
		
		'''CREATE TABLE bjcp_categories (_id INTEGER PRIMARY KEY, name text)''',
		'''CREATE TABLE bjcp_subcategories (_id integer primary key, 
			bjcp_category_id integer, display_id text, name text, aroma text, 
			appearance text, flavor text, mouthfeel text, impression text, 
			comments text, ingredients text, og_low numeric, og_high numeric, 
			fg_low numeric, fg_high numeric, ibu_low integer, ibu_high integer, 
			srm_low integer, srm_high integer, abv_low numeric, abv_high numeric, 
			examples text, 
			FOREIGN KEY(bjcp_category_id) REFERENCES bjcp_categories(_id));''',

		'''CREATE TABLE srm_colors (_id INTEGER PRIMARY KEY, srm numeric, r integer,
			g integer, b integer);''',
			
		'''CREATE TABLE hops (_id INTEGER PRIMARY KEY, name text, origin text, alpha_low numeric,
			alpha_high numeric, notes text);''',
		'''CREATE TABLE hops_substitutes (_id INTEGER PRIMARY KEY, hop_id integer,
			substitute_id integer, 
			FOREIGN KEY(hop_id) REFERENCES hops(_id), 
			FOREIGN KEY(substitute_id) REFERENCES hops(_id));''',
		
		'''CREATE TABLE fermentable_types (_id INTEGER PRIMARY KEY, type text);''',
		'''CREATE TABLE fermentables (_id INTEGER PRIMARY KEY, type_id integer,
			supplier text, name text, notes text, color numeric);'''
		]

	try:
		os.remove(sqlite_dbpath)
	except:
		pass

	conn = sqlite3.connect(sqlite_dbpath)
	cur = conn.cursor()
	
	for stmt in schema:
		cur.execute(stmt)
		
	cur.execute('INSERT INTO db_version (version) VALUES (?)', (version, ))
	
	conn.commit()
	cur.close()

def convert_bjcp_from_json():
	f = open(bjcp_json, 'r')
	jin = json.loads(f.read())

	conn = sqlite3.connect(sqlite_dbpath)
	cur = conn.cursor()

	for j in jin['categories']:
		cur.execute('insert into bjcp_categories (name) VALUES (?)',
			(j['name'], ))
		category_id = cur.lastrowid
		for k in j['subcategories']:
			cur.execute('''insert into bjcp_subcategories (bjcp_category_id, display_id, name,
			aroma, appearance, flavor, mouthfeel, impression, comments,
			ingredients, og_low, og_high, fg_low, fg_high, srm_low, srm_high, ibu_low, ibu_high,
			abv_low, abv_high, examples) VALUES
			(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)''', 
					(category_id, k['id'], k['name'], k['aroma'], k['appearance'],
					k['flavor'], k['mouthfeel'], k['impression'], k['comments'],
					k['ingredients'], k['og_low'], k['og_high'], k['fg_low'],
					k['fg_high'], k['srm_low'], k['srm_high'], k['ibu_low'], 
					k['ibu_high'], k['abv_low'], k['abv_high'], k['examples']))

	conn.commit()
	cur.close()
	
def get_hop_id(conn, name):
	hop_id = 0
	cur = conn.cursor()
	cur.execute('select _id from hops where name = ?', (name, ))
	row = cur.fetchone()
	if row != None:		
		hop_id = row[0]
	cur.close()
	return hop_id
	
def get_fermentable_type_id(conn, type):
	ferm_id = 0
	cur = conn.cursor()
	cur.execute('select _id from fermentable_types where type = ?', (type, ))
	row = cur.fetchone()
	if row != None:		
		ferm_id = row[0]
	cur.close()
	return ferm_id
	
def convert_hops_from_csv():
	conn = sqlite3.connect(sqlite_dbpath)
	cur = conn.cursor()
	
	hops_file = open(hops_csv, 'rb')
	hops = unicodecsv.reader(hops_file, delimiter=',', quotechar='"', encoding='utf-8')
	cols = [col.lower() for col in hops.next()]
		
	# do the initial insert
	for row in hops:
		alphas = row[cols.index('alpharange')].split('-')
		alpha_low = alphas[0]
		if len(alphas) > 1:
			alpha_high = alphas[1]
		else:
			alpha_high = alpha_low
		cur.execute('''insert into hops (name, origin, alpha_low, alpha_high, notes) 
			VALUES (?, ?, ?, ?, ?)''',
				(row[cols.index('name')].strip(), row[cols.index('origin')].strip(),
				alpha_low, alpha_high, row[cols.index('notes')].strip()))

	conn.commit()
				
	hops_file.seek(0)
	hops.next() # skip the column headers
	
	for row in hops:
		hop_id = get_hop_id(conn, row[cols.index('name')].strip())
		if len(row) > cols.index('substitutes'):
			subs = row[cols.index('substitutes')].split(',')
			for sub in subs:
				sub = sub.strip()
				sub_id = get_hop_id(conn, sub)
				if sub_id != 0:
					cur.execute('''insert into hops_substitutes (hop_id, substitute_id)
						VALUES (?, ?)''', (hop_id, sub_id))			
				else:
					print "Couldn't find %s for %s" % \
						(sub, row[cols.index('name')].strip())
	conn.commit()
	cur.close()
	
def convert_fermentables_from_csv():
	conn = sqlite3.connect(sqlite_dbpath)
	cur = conn.cursor()
	
	ferms_file = open(fermentables_csv, 'rb')
	ferms = unicodecsv.reader(ferms_file, delimiter=',', quotechar='"', encoding='utf-8')
	cols = [col.lower() for col in ferms.next()]
		
	for row in ferms:
		type_id = get_fermentable_type_id(conn, row[cols.index('type')].strip())
		if type_id == 0:
			cur.execute('''insert into fermentable_types (type) VALUES (?)''', 
				(row[cols.index('type')].strip(), ))
			type_id = cur.lastrowid
		cur.execute('''insert into fermentables (type_id, supplier, name, 
			notes, color) VALUES (?, ?, ?, ?, ?)''',
				(type_id, row[cols.index('supplier')].strip(),
				row[cols.index('name')].strip(), row[cols.index('notes')].strip(), 
				row[cols.index('color')].strip()))

	conn.commit()
	cur.close()

def convert_srm_from_csv():
	conn = sqlite3.connect(sqlite_dbpath)
	cur = conn.cursor()
	
	srm_file = open(srm_csv, 'rb')
	srms = unicodecsv.reader(srm_file, delimiter=',', quotechar='"', encoding='utf-8')
	cols = [col.lower() for col in srms.next()]
		
	for row in srms:
		cur.execute('''insert into srm_colors (srm, r, g, b) VALUES (?, ?, ?, ?)''',
				(row[cols.index('srm')].strip(), row[cols.index('r')].strip(), 
				row[cols.index('g')].strip(), row[cols.index('b')].strip()))

	conn.commit()
	cur.close()
	
if __name__ == '__main__':
	setup_sqlite()
	convert_bjcp_from_json()
	convert_hops_from_csv()
	convert_fermentables_from_csv()
	convert_srm_from_csv()