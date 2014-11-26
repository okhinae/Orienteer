package ru.ydn.orienteer.modules;

import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import ru.ydn.orienteer.CustomAttributes;
import ru.ydn.orienteer.OrienteerWebApplication;
import ru.ydn.orienteer.OrienteerWebSession;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OSecurityShared;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

@Singleton
public class PerspectivesModule extends AbstractOrienteerModule
{
	public static final String OCLASS_PERSPECTIVE="OPerspective";
	public static final String OCLASS_ITEM = "OPerspectiveItem";
	
	public static final String DEFAULT_PERSPECTIVE = "Default";
	

	public PerspectivesModule()
	{
		super("perspectives", 1);
	}

	@Override
	public void onInstall(OrienteerWebApplication app, ODatabaseDocument db) {
		OSchema schema = db.getMetadata().getSchema();
		OClass perspectiveClass = mergeOClass(schema, OCLASS_PERSPECTIVE);
		OClass itemClass = mergeOClass(schema, OCLASS_ITEM);
		mergeOProperty(perspectiveClass, "name", OType.STRING);
		mergeOProperty(perspectiveClass, "icon", OType.STRING);
		mergeOProperty(perspectiveClass, "homeUrl", OType.STRING);
		OProperty menu = mergeOProperty(perspectiveClass, "menu", OType.LINKLIST, "table").setLinkedClass(itemClass);
		mergeOProperty(perspectiveClass, "footer", OType.STRING, "textarea");
		assignNameAndParent(perspectiveClass, "name", null);
		switchDisplayable(perspectiveClass, true, "name", "homeUrl");
		orderProperties(perspectiveClass, "name", "icon", "homeUrl", "footer", "menu");
		mergeOIndex(perspectiveClass, OCLASS_PERSPECTIVE+".name", INDEX_TYPE.UNIQUE, "name");
		
		mergeOProperty(itemClass, "name", OType.STRING);
		mergeOProperty(itemClass, "icon", OType.STRING);
		mergeOProperty(itemClass, "url", OType.STRING);
		OProperty perspective = mergeOProperty(itemClass, "perspective", OType.LINK).setLinkedClass(perspectiveClass);
		assignNameAndParent(itemClass, "name", "perspective");
		switchDisplayable(itemClass, true, "name", "icon", "url");
		orderProperties(itemClass, "name", "perspective", "icon", "url");
		
		CustomAttributes.PROP_INVERSE.setValue(menu, perspective);
		CustomAttributes.PROP_INVERSE.setValue(perspective, menu);
		
		OClass identityClass = schema.getClass(OSecurityShared.IDENTITY_CLASSNAME);
		mergeOProperty(identityClass, "perspective", OType.LINK).setLinkedClass(perspectiveClass);
	}
	
	public ODocument getDefaultPerspective(ODatabaseDocument db, OUser user)
	{
		if(user!=null)
		{
			Object perspectiveObj = user.getDocument().field("perspective");
			if(perspectiveObj!=null && perspectiveObj instanceof OIdentifiable) 
				return (ODocument)((OIdentifiable)perspectiveObj).getRecord();
			Set<ORole> roles = user.getRoles();
			ODocument perspective = null;
			for (ORole oRole : roles)
			{
				perspective = getPerspectiveForORole(oRole);
				if(perspective!=null) return perspective;
			}
		}
		List<ODocument> defaultPerspectives = db.query(new OSQLSynchQuery<ODocument>("select from "+OCLASS_PERSPECTIVE+" where name=?"), DEFAULT_PERSPECTIVE);
		return defaultPerspectives==null || defaultPerspectives.size()<1?null:defaultPerspectives.get(0);
	}
	
	private ODocument getPerspectiveForORole(ORole role)
	{
		if(role==null) return null;
		Object perspectiveObj = role.getDocument().field("perspective");
		if(perspectiveObj!=null && perspectiveObj instanceof OIdentifiable) 
			return (ODocument)((OIdentifiable)perspectiveObj).getRecord();
		else
		{
			ORole parentRole = role.getParentRole();
			if(parentRole!=null && !parentRole.equals(role))
			{
				return getPerspectiveForORole(parentRole);
			}
			else
			{
				return null;
			}
		}
	}

	@Override
	public void onInitialize(OrienteerWebApplication app, ODatabaseDocument db) {
		OSchema schema  = db.getMetadata().getSchema();
		if(schema.getClass(OCLASS_PERSPECTIVE)==null || schema.getClass(OCLASS_ITEM)==null)
		{
			//Repair
			onInstall(app, db);
		}
		if(getDefaultPerspective(db, null)==null)
		{
			ODocument perspective = new ODocument(OCLASS_PERSPECTIVE);
			perspective.field("name", DEFAULT_PERSPECTIVE);
			perspective.field("homeUrl", "/classes");
			perspective.save();
			
			ODocument item = new ODocument(OCLASS_ITEM);
			item.field("name", "Users");
			item.field("icon", "users");
			item.field("url", "/browse/OUser");
			item.field("perspective", perspective);
			item.save();
			
			item = new ODocument(OCLASS_ITEM);
			item.field("name", "Roles");
			item.field("icon", "users");
			item.field("url", "/browse/ORole");
			item.field("perspective", perspective);
			item.save();
			
			item = new ODocument(OCLASS_ITEM);
			item.field("name", "Classes");
			item.field("icon", "cubes");
			item.field("url", "/classes");
			item.field("perspective", perspective);
			item.save();
		}
	}

}
