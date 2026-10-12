<!-- gems-ai-rule: {"id":"java.architecture","version":"2.0.0","status":"candidate"} -->
# Java architecture

The repository MUST declare its actual Maven topology and dependency direction as local facts. The
canonical contract does not prescribe consumer module names. Domain models and DTOs belong in the
declared model boundary; controllers, services, repositories, and business listeners belong in the
core boundary; executable configuration, migrations, and packaging belong only in the deploy
boundary. An events module, when present, contains event contracts and MUST NOT depend back on core.

Cross-domain reads may use the declared read boundary. Cross-domain business writes SHOULD use an
event or an explicitly approved application contract. Architecture gates MUST validate the real
reactor rather than a template topology.
