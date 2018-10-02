package ci.gouv.dgbf.system.workflow.server.representation.impl.integration;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import org.cyk.utility.server.representation.AbstractEntityFromPersistenceEntityDto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@XmlRootElement @Getter @Setter @Accessors(chain=true) @NoArgsConstructor
public class MyEntityDto extends AbstractEntityFromPersistenceEntityDto implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Override
	public MyEntityDto setCode(String code) {
		return (MyEntityDto) super.setCode(code);
	}
	
	@Override
	public String toString() {
		return getIdentifier()+":"+getCode();
	}
}
