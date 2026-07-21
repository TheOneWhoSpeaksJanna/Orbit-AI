#!/usr/bin/env node
/**
 * Test script for the Hidden Channels plugin.
 * Simulates what the Vendetta/Revenge/ShiggyCord mod does:
 * 1. Fetch manifest.json from the install URL
 * 2. Read the "main" field
 * 3. Resolve the full JS URL
 * 4. Fetch and evaluate the JS
 * 5. Verify the exports (onLoad, onUnload)
 */

const https = require('https');
const path = require('path');

function fetch(url) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'Accept': '*/*' } }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        resolve({
          status: res.statusCode,
          contentType: res.headers['content-type'] || '',
          cors: res.headers['access-control-allow-origin'] || 'missing',
          data,
          resolvedUrl: res.responseUrl || url,
        });
      });
    }).on('error', reject);
  });
}

async function testPlugin(manifestUrl) {
  console.log(`\n═══════════════════════════════════════`);
  console.log(`TESTING: ${manifestUrl}`);
  console.log(`═══════════════════════════════════════\n`);

  // Step 1: Fetch manifest
  console.log(`📋 Step 1: Fetch manifest.json`);
  const manifestResp = await fetch(manifestUrl);
  
  console.log(`   Status: ${manifestResp.status}`);
  console.log(`   Content-Type: ${manifestResp.contentType}`);
  console.log(`   CORS: ${manifestResp.cors}`);
  
  if (manifestResp.status !== 200) {
    console.log(`   ❌ FAIL: Manifest returned HTTP ${manifestResp.status}`);
    return false;
  }
  
  let manifest;
  try {
    manifest = JSON.parse(manifestResp.data);
    console.log(`   ✓ Valid JSON`);
    console.log(`   Plugin: "${manifest.name}"`);
    console.log(`   Main: "${manifest.main}"`);
  } catch (e) {
    console.log(`   ❌ FAIL: Invalid JSON: ${e.message}`);
    return false;
  }
  
  // Step 2: Resolve main JS URL relative to manifest URL
  const baseUrl = manifestUrl.substring(0, manifestUrl.lastIndexOf('/') + 1);
  const jsUrl = new URL(manifest.main, manifestUrl).href;
  
  console.log(`\n📋 Step 2: Resolve main JS URL`);
  console.log(`   Base URL: ${baseUrl}`);
  console.log(`   Main: "${manifest.main}"`);
  console.log(`   Full JS URL: ${jsUrl}`);
  
  // Step 3: Fetch JS
  console.log(`\n📋 Step 3: Fetch compiled JS`);
  const jsResp = await fetch(jsUrl);
  
  console.log(`   Status: ${jsResp.status}`);
  console.log(`   Content-Type: ${jsResp.contentType}`);
  console.log(`   CORS: ${jsResp.cors}`);
  console.log(`   Size: ${(jsResp.data.length / 1024).toFixed(1)} KB`);
  
  if (jsResp.status !== 200) {
    console.log(`   ❌ FAIL: JS returned HTTP ${jsResp.status}`);
    return false;
  }
  
  // Step 4: Validate JS structure
  console.log(`\n📋 Step 4: Validate JS structure`);
  
  const hasIIFE = jsResp.data.includes('function(exports, metro, common, patcher');
  console.log(`   IIFE wrapper: ${hasIIFE ? '✓' : '✗'}`);
  
  const hasVendettaGlobals = jsResp.data.includes('vendetta.metro') && 
                              jsResp.data.includes('vendetta.patcher');
  console.log(`   Vendetta globals: ${hasVendettaGlobals ? '✓' : '✗'}`);
  
  const hasExports = jsResp.data.includes('exports.default');
  console.log(`   exports.default: ${hasExports ? '✓' : '✗'}`);
  
  const hasOnLoad = jsResp.data.includes('onLoad');
  const hasOnUnload = jsResp.data.includes('onUnload');
  console.log(`   onLoad/onUnload: ${hasOnLoad && hasOnUnload ? '✓' : '✗'}`);
  
  const noESMImports = !jsResp.data.includes('import {');
  console.log(`   No ESM imports: ${noESMImports ? '✓' : '✗'}`);
  
  // Step 5: Try to evaluate (syntax check)
  console.log(`\n📋 Step 5: Syntax validation`);
  try {
    // We can't fully evaluate because vendetta globals don't exist,
    // but we can check if the Function constructor rejects the code
    // due to syntax errors
    new Function('vendetta', jsResp.data + '\n//# sourceURL=plugin-test');
    console.log(`   ✓ Syntax OK (Function constructor accepts it)`);
  } catch (e) {
    console.log(`   ❌ Syntax error: ${e.message}`);
    return false;
  }
  
  // All checks passed
  const allChecks = hasIIFE && hasVendettaGlobals && hasExports && 
                    hasOnLoad && hasOnUnload && noESMImports;
  
  console.log(`\n═══════════════════════════════════════`);
  if (allChecks) {
    console.log(`✅ ALL CHECKS PASSED - Plugin is valid`);
  } else {
    console.log(`❌ SOME CHECKS FAILED`);
  }
  console.log(`═══════════════════════════════════════\n`);
  
  return allChecks;
}

async function main() {
  const urls = [
    // Commit-hash pinned jsDelivr manifest
    'https://cdn.jsdelivr.net/gh/TheOneWhoSpeaksJanna/Orbit-AI@67ce5ad/plugins/hidden-channels/manifest.json',
    // raw.githubusercontent.com manifest
    'https://raw.githubusercontent.com/TheOneWhoSpeaksJanna/Orbit-AI/main/plugins/hidden-channels/manifest.json',
  ];
  
  let allPassed = true;
  for (const url of urls) {
    const passed = await testPlugin(url);
    if (!passed) allPassed = false;
  }
  
  console.log(`\n═══ OVERALL: ${allPassed ? '✅ ALL TESTS PASSED' : '❌ SOME TESTS FAILED'} ═══\n`);
  process.exit(allPassed ? 0 : 1);
}

main().catch(e => {
  console.error('Test error:', e);
  process.exit(1);
});
