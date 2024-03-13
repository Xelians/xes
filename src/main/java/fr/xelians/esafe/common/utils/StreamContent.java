/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.utils;

import java.io.InputStream;

public record StreamContent(String name, String mimetype, InputStream inputStream) {}
