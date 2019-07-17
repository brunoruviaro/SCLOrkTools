#include "Config.hpp"

// Flatbuffer include generated by calling flatc on the Flatbuffer schema file FlatConfig.fbs.
#include "schemas/FlatConfig_generated.h"

namespace Confab {

Config::Config(const Common::Version& version) :
    m_version(version) {
}

const SizedPointer Config::flatten() {
    m_builder.reset(new flatbuffers::FlatBufferBuilder);
    m_configBuilder.reset(new Data::FlatConfigBuilder(*m_builder));
    m_configBuilder->add_versionMajor(m_version.major());
    m_configBuilder->add_versionMinor(m_version.minor());
    m_configBuilder->add_versionPatch(m_version.patch());
    auto config = m_configBuilder->Finish();
    m_builder->Finish(config, Data::FlatConfigIdentifier());
    return SizedPointer(m_builder->GetBufferPointer(), m_builder->GetSize());
}

// static
bool Config::Verify(const RecordPtr record) {
    auto verifier = flatbuffers::Verifier(record->data().data(), record->data().size());
    return Data::VerifyFlatConfigBuffer(verifier);
}

// static
const Config Config::LoadConfig(const RecordPtr record) {
    auto flatConfig = Data::GetFlatConfig(record->data().data());
    return Config(record, flatConfig);
}

Config::Config(const RecordPtr record, const Data::FlatConfig* flatConfig) :
    m_record(record),
    m_flatConfig(flatConfig),
    m_version(m_flatConfig->versionMajor(), m_flatConfig->versionMinor(), m_flatConfig->versionPatch()) {
}

}  // namespace Confab
