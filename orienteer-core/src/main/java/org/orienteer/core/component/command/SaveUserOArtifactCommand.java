package org.orienteer.core.component.command;

import com.google.common.base.Strings;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.eclipse.aether.artifact.Artifact;
import org.orienteer.core.boot.loader.util.OrienteerClassLoaderUtil;
import org.orienteer.core.boot.loader.util.artifact.OArtifact;
import org.orienteer.core.boot.loader.util.artifact.OArtifactReference;
import org.orienteer.core.component.property.DisplayMode;
import org.orienteer.core.component.structuretable.OrienteerStructureTable;

import java.io.File;
import java.nio.file.Path;

/**
 * Save user configure of {@link OArtifact}
 */
public class SaveUserOArtifactCommand extends AbstractSaveOArtifactCommand {

    public SaveUserOArtifactCommand(OrienteerStructureTable<OArtifact, ?> table, IModel<DisplayMode> displayModeModel, Label feedback) {
        super(table, displayModeModel, feedback);
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        IModel<OArtifact> model = getModel();
        if (model == null) {
            sendErrorFeedback(target, new ResourceModel(ERROR));
            return;
        }
        OArtifact artifact = model.getObject();
        File artifactFile = artifact.getArtifactReference().getFile();
        if (artifactFile != null) {
            if (isUserArtifactValid(target, artifact)) {
                Path path = OrienteerClassLoaderUtil
                        .moveJarFileToArtifactsFolder(artifactFile.toPath(), artifactFile.getName());
                if (path!=null) {
                    artifact.getArtifactReference().setFile(path.toFile());
                    OrienteerClassLoaderUtil.updateOArtifactInMetadata(artifact);
                    artifact.setDownloaded(true);
                }
            } else OrienteerClassLoaderUtil.deleteOArtifactFile(artifact);
        } else if (isUserArtifactValid(target, artifact)) {
            resolveUserArtifact(target, artifact);
        }
    }

    private void resolveUserArtifact(AjaxRequestTarget target, OArtifact oArtifact) {
        String repository = oArtifact.getArtifactReference().getRepository();
        OArtifactReference artifactReference = oArtifact.getArtifactReference();
        Artifact artifact;
        if (!Strings.isNullOrEmpty(repository)) {
            artifact = OrienteerClassLoaderUtil.downloadArtifact(artifactReference.toAetherArtifact(), repository);
        } else artifact = OrienteerClassLoaderUtil.downloadArtifact(artifactReference.toAetherArtifact());

        if (artifact!=null) {
            artifactReference.setFile(artifact.getFile());
            OrienteerClassLoaderUtil.updateOArtifactInMetadata(oArtifact);
            oArtifact.setDownloaded(true);
        } else {
            sendErrorFeedback(target, new ResourceModel(DOWNLOAD_ERROR));
        }
    }

}
